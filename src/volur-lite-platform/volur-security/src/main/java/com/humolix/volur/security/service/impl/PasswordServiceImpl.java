package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.PasswordService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private final UserTrustedDeviceService userTrustedDeviceService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FlowCryptoService flowCryptoService;
    private final PasswordEncoder passwordEncoder;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final SystemSettingService systemSettingService;
    private final StringRedisTemplate redisTemplate;

    private static final String VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX = "vredis:auth:login:attempts:";
    private static final String VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX = "vredis:auth:login:lock:";

    @Override
    public FlowResponseVO verifyPassword(PasswordChallengeRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        transitionLockManager.validateTransLock(dsh, tl, "pwd");

        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        String clientIp = RequestUtil.getClientIp();
        String flowName = session.getFlow() != null ? session.getFlow().getFlowName() : FlowNames.VLIF_WEB_LOGIN.getFlowName();
        String flowEntry = session.getFlow() != null ? session.getFlow().getFlowEntry() : FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry();
        String plainPassword = request.getPassword().startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(request.getPassword(), flowName, flowEntry, session.getDsh())
                : request.getPassword();

        String cleanIdentifier = identifier.trim();
        String lusername = cleanIdentifier.toLowerCase();

        int maxAttempts = systemSettingService.getInt("system.auth.max_login_attempts", 5);
        String lockKey = VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX + lusername;
        String attemptsKey = VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX + lusername;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
        }

        User user = userRepository.findByUsername(cleanIdentifier)
                .or(() -> userRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElse(null);

        if (user == null || !passwordEncoder.matches(plainPassword, user.getPassword())) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            riskEvaluationEngine.recordAccountFailure(cleanIdentifier, CaptchaScene.LOGIN);

            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(30));
            }
            if (attempts != null && attempts >= maxAttempts) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(15));
                redisTemplate.delete(attemptsKey);
                throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
            }
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被封禁");
        }

        boolean isMaintenance = systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false);
        if (isMaintenance) {
            boolean isAdmin = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;
            if (!isAdmin) {
                String notice = systemSettingService.getSettingValue(
                        SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE,
                        "系统正在全站维护升级中，暂仅限管理员登录"
                );
                throw new MaintenanceModeException(notice);
            }
        }

        redisTemplate.delete(attemptsKey);
        riskEvaluationEngine.recordSuccess(clientIp, CaptchaScene.LOGIN);
        riskEvaluationEngine.recordAccountSuccess(cleanIdentifier, CaptchaScene.LOGIN);

        String deviceHash = session.getRequestDeviceParams() != null
                ? session.getRequestDeviceParams().getDeviceHash()
                : null;
        if (!StringUtils.hasText(deviceHash) && session.getRequestDeviceParams() != null) {
            deviceHash = session.getRequestDeviceParams().getFingerprint();
        }

        boolean isTrusted = StringUtils.hasText(deviceHash)
                && userTrustedDeviceService.isTrustedDevice(user.getId(), deviceHash);

        boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true);
        boolean mailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false);

        if (requireEmailVerification && mailEnabled && !isTrusted && StringUtils.hasText(user.getEmail())) {
            if (session.getFlow() != null) {
                session.getFlow().addWaitingStepFirst(SigninFlow.STEP_ETOTP);
                sessionManager.saveSession(session);
            }
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), true);

            int nextCid = cid + 1;
            String etotpTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, "pwd", nextCid, "etotp", cleanIdentifier
            );
            return FlowResponseVO.builder()
                    .dsh(dsh)
                    .tl(etotpTL)
                    .cid(nextCid)
                    .nextStep("etotp")
                    .status("CHALLENGE")
                    .identifier(cleanIdentifier)
                    .build();
        }

        if (StringUtils.hasText(deviceHash)) {
            String osFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getOsFamily() : null;
            String browserFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getBrowserFamily() : null;
            userTrustedDeviceService.recordLoginDevice(user.getId(), deviceHash, osFamily, browserFamily, clientIp);
        }

        if (session.getFlow() != null) {
            session.getFlow().completeStep("pwd");
            sessionManager.saveSession(session);
        }

        int nextCid = cid + 1;
        String authTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, "pwd", nextCid, SigninFlow.STEP_AUTHENTICATE, cleanIdentifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(authTL)
                .cid(nextCid)
                .nextStep(SigninFlow.STEP_AUTHENTICATE)
                .status("CHALLENGE")
                .identifier(cleanIdentifier)
                .build();
    }

}
