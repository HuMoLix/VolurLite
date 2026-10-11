package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.TOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.TotpEnableRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.TotpEnableResponseVO;
import com.humolix.volur.security.domain.vo.TotpSetupVO;
import com.humolix.volur.security.domain.vo.TotpStatusVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.TotpService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.security.util.TotpUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpServiceImpl implements TotpService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final StringRedisTemplate redisTemplate;
    private final SystemSettingService systemSettingService;
    private final UserRepository userRepository;
    private final SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private final UserTrustedDeviceService userTrustedDeviceService;
    private final RiskEvaluationEngine riskEvaluationEngine;

    private static final String VREDIS_SECURITY_TOTP_TEMP_KEY_PREFIX = "vredis:security:totp:temp:";
    private static final Duration TOTP_TEMP_TTL = Duration.ofMinutes(10);

    @Override
    public TotpSetupVO setupTotp(String dsh, String tl) {
        transitionLockManager.validateTransLock(dsh, tl, "pt/options");
        Session session = sessionManager.getSession(dsh);

        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (!StringUtils.hasText(username) && session.getFlow() != null) {
            username = session.getFlow().getIdentifier();
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法定位当前会话绑定的用户，请重新开始注册");
        }

        String secret = TotpUtil.generateSecret();
        String redisKey = VREDIS_SECURITY_TOTP_TEMP_KEY_PREFIX + dsh;
        redisTemplate.opsForValue().set(redisKey, secret, TOTP_TEMP_TTL);

        String appName = systemSettingService.getSettingValue("system.security.webauthn.rp_name", "Volur");
        String otpAuthUrl = TotpUtil.generateOtpAuthUri(appName, username, secret);

        return TotpSetupVO.builder()
                .secret(secret)
                .otpAuthUrl(otpAuthUrl)
                .account(username)
                .issuer(appName)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TotpEnableResponseVO enableTotp(TotpEnableRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "pt/options");
        Session session = sessionManager.getSession(request.getDsh());

        String redisKey = VREDIS_SECURITY_TOTP_TEMP_KEY_PREFIX + request.getDsh();
        String secret = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "动态口令配置会话已失效，请重新生成二维码");
        }

        boolean isValid = TotpUtil.verifyCode(secret, request.getCode(), 1);
        if (!isValid) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "动态验证码错误或已过期，请重试");
        }

        Object userIdObj = sessionManager.getMetadata(session.getDsh(), "registeredUserId");
        Long userId = null;
        if (userIdObj instanceof Number num) {
            userId = num.longValue();
        } else if (userIdObj instanceof String str && StringUtils.hasText(str)) {
            try {
                userId = Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (userId == null && StringUtils.hasText(username)) {
            userId = userRepository.findByUsername(username).map(User::getId).orElse(null);
        }
        if (userId == null && session.getFlow() != null && StringUtils.hasText(session.getFlow().getIdentifier())) {
            userId = userRepository.findByUsername(session.getFlow().getIdentifier()).map(User::getId).orElse(null);
        }
        if (userId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法定位当前注册用户，请重新开始注册");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setTotpSecret(secret);
        user.setIsTotpEnabled(true);
        userRepository.save(user);

        redisTemplate.delete(redisKey);
        sessionManager.setMetadata(session.getDsh(), "totpEnabled", true);

        return TotpEnableResponseVO.builder()
                .success(true)
                .message("动态身份验证器绑定成功")
                .build();
    }

    @Override
    public FlowResponseVO verifyTotp(TOTPChallengeRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);

        String currentStep = session.getFlow() != null ? session.getFlow().getCurrentStep() : "totp";
        transitionLockManager.validateTransLock(dsh, tl, currentStep);

        String username = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(username)) {
            username = (String) sessionManager.getMetadata(dsh, "username");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        String cleanIdentifier = username.trim();
        User user = userRepository.findByUsername(cleanIdentifier)
                .or(() -> userRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在"));

        if (!Boolean.TRUE.equals(user.getIsTotpEnabled()) || !StringUtils.hasText(user.getTotpSecret())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定动态身份验证器");
        }

        String clientIp = RequestUtil.getClientIp();
        boolean isValid = TotpUtil.verifyCode(user.getTotpSecret(), request.getCode(), 1);
        if (!isValid) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "动态验证码错误或已过期");
        }

        riskEvaluationEngine.recordSuccess(clientIp, CaptchaScene.LOGIN);
        riskEvaluationEngine.recordAccountSuccess(cleanIdentifier, CaptchaScene.LOGIN);

        String deviceHash = session.getRequestDeviceParams() != null
                ? session.getRequestDeviceParams().getDeviceHash()
                : null;
        if (!StringUtils.hasText(deviceHash) && session.getRequestDeviceParams() != null) {
            deviceHash = session.getRequestDeviceParams().getFingerprint();
        }
        if (StringUtils.hasText(deviceHash)) {
            String osFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getOsFamily() : null;
            String browserFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getBrowserFamily() : null;
            userTrustedDeviceService.recordLoginDevice(user.getId(), deviceHash, osFamily, browserFamily, clientIp);
        }

        SigninFlow signinFlow = SigninFlow.from(session.getFlow());
        signinFlow.completeStep(currentStep);
        session.setFlow(signinFlow);
        sessionManager.saveSession(session);

        int nextCid = cid + 1;
        String authTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, currentStep, nextCid, SigninFlow.STEP_AUTHENTICATE, cleanIdentifier
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

    @Override
    public TotpStatusVO getTotpStatus(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        return TotpStatusVO.builder()
                .enabled(Boolean.TRUE.equals(user.getIsTotpEnabled()))
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTotp(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!Boolean.TRUE.equals(user.getIsTotpEnabled())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "动态身份验证器尚未启用");
        }

        user.setIsTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }
}
