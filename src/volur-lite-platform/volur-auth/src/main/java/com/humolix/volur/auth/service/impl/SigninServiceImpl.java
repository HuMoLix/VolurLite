package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.ChallengeOptionVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;

import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private final TransitionLockManager transitionLockManager;
    private final RecaptchaService recaptchaService;
    private final FlowCryptoService flowCryptoService;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final CaptchaSDK captchaSDK;
    private final UserRepository userRepository;
    private final Sm3PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SystemSettingService systemSettingService;
    private final RedisOps redisOps;
    private final StringRedisTemplate redisTemplate;
    private final AuthCookieService authCookieService;

    private static final String VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX = "vredis:auth:login_attempts:";
    private static final String VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX = "vredis:auth:login_lock:";
    private static final String VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX = "vredis:auth:identifier_failures:";
    private static final Duration IDENTIFIER_FAILURE_TTL = Duration.ofMinutes(15);
    private static final long IDENTIFIER_RECAPTCHA_THRESHOLD = 5L;
    private static final long IDENTIFIER_SLIDER_THRESHOLD = 10L;

    @Override
    public SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request) {
        String clientIp = RequestUtil.getClientIp();
        String userAgent = RequestUtil.getUserAgent();
        String rawFp = StringUtils.hasText(request.getDeviceFingerprint())
                ? request.getDeviceFingerprint()
                : RequestUtil.getDeviceFingerprint();
        String identifier = request.getIdentifier().trim();
        String subjectKey = buildClientSubjectKey(clientIp, rawFp);

        AuthFlowSession session = transitionLockManager.createOrGetSession(
                request.getDsh(), "VlifWebLogin", "WebServiceLogin", clientIp, userAgent
        );
        session.setIdentifier(identifier);
        session.setClientIp(clientIp);
        session.setUserAgent(userAgent);
        session.setDeviceFingerprint(rawFp);

        long failures = getIdentifierFailures(subjectKey);

        RiskLevel riskLevel = riskEvaluationEngine.evaluateComprehensiveRisk(
                clientIp, identifier, rawFp, CaptchaScene.LOGIN
        );
        session.setRiskLevel(riskLevel.name());

        String lowerIdentifier = identifier.toLowerCase();
        boolean isEnvSuspicious = riskLevel == RiskLevel.HIGH
                || lowerIdentifier.contains("bot")
                || lowerIdentifier.contains("captcha")
                || lowerIdentifier.contains("robot");

        if (failures >= IDENTIFIER_RECAPTCHA_THRESHOLD || isEnvSuspicious) {
            String tl = transitionLockManager.advanceTransitionTicket(
                    session.getDsh(), null, 0, "identifier", 1, "recaptcha", identifier
            );
            return SignInFlowResponseVO.builder()
                    .dsh(session.getDsh())
                    .tl(tl)
                    .cid(1)
                    .nextStep("recaptcha")
                    .status("CHALLENGE")
                    .identifier(identifier)
                    .build();
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElse(null);

        if (user == null) {
            recordIdentifierFailure(subjectKey);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在");
        }

        resetIdentifierFailures(subjectKey);

        String tl = transitionLockManager.advanceTransitionTicket(
                session.getDsh(), null, 0, "identifier", 1, "pwd", identifier
        );
        return SignInFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(tl)
                .cid(1)
                .nextStep("pwd")
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public SignInFlowResponseVO challenge(SignInChallengeRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid();
        String challengeType = request.getChallengeType().trim().toLowerCase();

        AuthFlowSession session = transitionLockManager.getSession(dsh);
        String identifier = session.getIdentifier();
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        String clientIp = RequestUtil.getClientIp();

        switch (challengeType) {
            case "recaptcha" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "recaptcha");

                String ticket = StringUtils.hasText(request.getCaptchaTicket())
                        ? request.getCaptchaTicket()
                        : request.getCaptchaCode();

                if (!StringUtils.hasText(ticket) || !recaptchaService.validateAndBurnTicket(dsh, ticket)) {
                    riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "人机安全验证未通过或凭据已失效，请重试");
                }

                String subjectKey = buildClientSubjectKey(clientIp, session.getDeviceFingerprint());
                User user = userRepository.findByUsername(identifier)
                        .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                        .orElse(null);

                if (user == null) {
                    recordIdentifierFailure(subjectKey);
                    int nextCid = cid + 1;
                    String nextTL = transitionLockManager.advanceTransitionTicket(
                            dsh, tl, cid, "recaptcha", nextCid, "identifier", identifier
                    );
                    return SignInFlowResponseVO.builder()
                            .dsh(dsh)
                            .tl(nextTL)
                            .cid(nextCid)
                            .nextStep("identifier")
                            .status("CHALLENGE")
                            .identifier(identifier)
                            .errorMessage("用户名或邮箱不存在")
                            .build();
                }

                resetIdentifierFailures(subjectKey);
                riskEvaluationEngine.reduceRisk(clientIp);

                int nextCid = cid + 1;
                String pwdTL = transitionLockManager.advanceTransitionTicket(
                        dsh, tl, cid, "recaptcha", nextCid, "pwd", identifier
                );

                return SignInFlowResponseVO.builder()
                        .dsh(dsh)
                        .tl(pwdTL)
                        .cid(nextCid)
                        .nextStep("pwd")
                        .status("CHALLENGE")
                        .identifier(identifier)
                        .build();
            }

            case "pwd" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "pwd");

                if (!StringUtils.hasText(request.getPassword())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入密码");
                }

                String plainPassword = request.getPassword().startsWith("$V1$GCM$")
                        ? flowCryptoService.decrypt(request.getPassword(), session.getFlowName(), session.getFlowEntry(), session.getDsh())
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

                    long attempts = redisOps.incr(attemptsKey, 1, Duration.ofMinutes(30));
                    if (attempts >= maxAttempts) {
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

                boolean require2fa = lusername.contains("2fa") || lusername.contains("totp");
                if (require2fa) {
                    int nextCid = cid + 1;
                    String nextTL = transitionLockManager.advanceTransitionTicket(
                            dsh, tl, cid, "pwd", nextCid, "2fa", identifier
                    );
                    return SignInFlowResponseVO.builder()
                            .dsh(dsh)
                            .tl(nextTL)
                            .cid(nextCid)
                            .nextStep("2fa")
                            .status("CHALLENGE")
                            .identifier(identifier)
                            .build();
                }

                transitionLockManager.burnAndCompleteSession(dsh, tl, cid, "pwd");

                String jti = IdUtil.fastSimpleUUID();
                boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
                String accessToken = jwtUtil.generateAccessTokenFromUser(user, jti);
                String refreshToken = jwtUtil.generateRefreshTokenFromUser(user, jti, rememberMe);

                authCookieService.writeAuthCookies(accessToken, refreshToken, rememberMe);

                return SignInFlowResponseVO.builder()
                        .status("SUCCESS")
                        .identifier(cleanIdentifier)
                        .build();
            }

            case "2fa", "totp" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "2fa");

                if (!StringUtils.hasText(request.getVerificationCode())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入两步验证动态口令");
                }

                User user = userRepository.findByUsername(identifier)
                        .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

                transitionLockManager.burnAndCompleteSession(dsh, tl, cid, "2fa");

                String jti = IdUtil.fastSimpleUUID();
                boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
                String accessToken = jwtUtil.generateAccessTokenFromUser(user, jti);
                String refreshToken = jwtUtil.generateRefreshTokenFromUser(user, jti, rememberMe);

                authCookieService.writeAuthCookies(accessToken, refreshToken, rememberMe);

                return SignInFlowResponseVO.builder()
                        .status("SUCCESS")
                        .identifier(identifier)
                        .build();
            }

            case "device" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "device");

                int nextCid = cid + 1;
                String nextTL = transitionLockManager.advanceTransitionTicket(
                        dsh, tl, cid, "device", nextCid, "pwd", identifier
                );
                return SignInFlowResponseVO.builder()
                        .dsh(dsh)
                        .tl(nextTL)
                        .cid(nextCid)
                        .nextStep("pwd")
                        .status("CHALLENGE")
                        .identifier(identifier)
                        .build();
            }
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的安全挑战类型: " + challengeType);
    }

    private String buildClientSubjectKey(String clientIp, String deviceFingerprint) {
        String cleanIp = StringUtils.hasText(clientIp) ? clientIp.trim() : "127.0.0.1";
        String cleanFp = StringUtils.hasText(deviceFingerprint) ? deviceFingerprint.trim() : "unknown_fp";
        return DigestUtil.md5Hex(cleanIp + "#" + cleanFp);
    }

    private long getIdentifierFailures(String subjectKey) {
        String key = VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX + subjectKey;
        String val = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(val)) {
            return 0L;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void recordIdentifierFailure(String subjectKey) {
        String key = VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX + subjectKey;
        redisOps.incr(key, 1, IDENTIFIER_FAILURE_TTL);
    }

    private void resetIdentifierFailures(String subjectKey) {
        String key = VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX + subjectKey;
        redisTemplate.delete(key);
    }

    @Override
    public List<ChallengeOptionVO> getChallengeSelection(String dsh, String currentType) {
        String cleanCurrent = StringUtils.hasText(currentType) ? currentType.trim().toLowerCase() : "";

        String maskedEmail = null;
        String maskedPhone = null;

        if (StringUtils.hasText(dsh)) {
            try {
                AuthFlowSession session = transitionLockManager.getSession(dsh);
                if (session != null && StringUtils.hasText(session.getIdentifier())) {
                    String identifier = session.getIdentifier().trim();
                    User user = userRepository.findByUsername(identifier)
                            .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                            .orElse(null);
                    if (user != null) {
                        if (StringUtils.hasText(user.getEmail())) {
                            maskedEmail = maskEmail(user.getEmail());
                        }
                        if (StringUtils.hasText(user.getPhone())) {
                            maskedPhone = maskPhone(user.getPhone());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<ChallengeOptionVO> allOptions = new ArrayList<>();

        allOptions.add(ChallengeOptionVO.builder()
                .type("pk")
                .title("在您的手机或平板电脑上点按“是”")
                .subtitle("Volur 会向您的移动设备发送提示，以确认是您本人")
                .icon("device")
                .path("/v1/signin/challenge/pk")
                .build());

        allOptions.add(ChallengeOptionVO.builder()
                .type("etotp")
                .title("获取电子邮件验证码")
                .subtitle(maskedEmail != null ? "向邮箱 " + maskedEmail + " 发送一次性安全代码" : "向您的安全邮箱发送一次性安全代码")
                .icon("email")
                .path("/v1/signin/challenge/etotp")
                .build());

        allOptions.add(ChallengeOptionVO.builder()
                .type("totp")
                .title("在手机上获取动态验证码")
                .subtitle(maskedPhone != null ? "向手机号 " + maskedPhone + " 发送短信验证码" : "向您的绑定安全手机发送短信验证码")
                .icon("sms")
                .path("/v1/signin/challenge/totp")
                .build());

        allOptions.add(ChallengeOptionVO.builder()
                .type("atcr")
                .title("使用“身份验证器”应用")
                .subtitle("从您的身份验证器应用 (Google Authenticator) 获取动态验证代码")
                .icon("shield")
                .path("/v1/signin/challenge/atcr")
                .build());

        allOptions.add(ChallengeOptionVO.builder()
                .type("pwd")
                .title("输入您的密码")
                .subtitle("使用您的 Volur 账号密码验证身份")
                .icon("key")
                .path("/v1/signin/challenge/pwd")
                .build());

        if (StringUtils.hasText(cleanCurrent)) {
            List<ChallengeOptionVO> filtered = allOptions.stream()
                    .filter(opt -> !opt.getType().equalsIgnoreCase(cleanCurrent))
                    .collect(Collectors.toList());
            if (filtered.size() >= 4 && "pwd".equals(cleanCurrent)) {
                filtered.add(ChallengeOptionVO.builder()
                        .type("backup_code")
                        .title("输入 8 位备用验证码")
                        .subtitle("使用您保存的一次性安全备用码")
                        .icon("shield")
                        .path("/v1/signin/challenge/backup_code")
                        .build());
            }
            return filtered;
        }

        return allOptions;
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) {
            return email;
        }
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
