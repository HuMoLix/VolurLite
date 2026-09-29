package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.framework.security.flow.AuthFlowSession;
import com.humolix.volur.framework.security.flow.TransitionLockManager;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
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

/**
 * 阶段化登录流转业务服务实现 (Google Accounts 规范)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private final TransitionLockManager transitionLockManager;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final CaptchaSDK captchaSDK;
    private final UserRepository userRepository;
    private final Sm3PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SystemSettingService systemSettingService;
    private final RedisOps redisOps;
    private final StringRedisTemplate redisTemplate;

    private static final String VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX = "vredis:auth:login_attempts:";
    private static final String VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX = "vredis:auth:login_lock:";

    @Override
    public SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request) {
        String clientIp = RequestUtil.getClientIp();
        String userAgent = RequestUtil.getUserAgent();
        String identifier = request.getIdentifier();

        AuthFlowSession session = transitionLockManager.createOrGetSession(
                request.getDsh(), "VlifWebLogin", "WebServiceLogin", clientIp, userAgent
        );
        session.setIdentifier(identifier);

        // 账号与设备综合风控评估
        RiskLevel riskLevel = riskEvaluationEngine.evaluateComprehensiveRisk(
                clientIp, identifier, request.getDeviceFingerprint(), CaptchaScene.LOGIN
        );
        session.setRiskLevel(riskLevel.name());

        String lowerIdentifier = identifier.toLowerCase();
        boolean forceRecaptcha = riskLevel == RiskLevel.HIGH
                || lowerIdentifier.contains("bot")
                || lowerIdentifier.contains("captcha")
                || lowerIdentifier.contains("robot");

        if (forceRecaptcha) {
            // 环境异常 / 爬虫风险：要求人机验证，推进至 cid=1，签发初始 TL
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
        } else {
            // 正常低风险环境：密码挑战 (/challenge/pwd)，推进至 cid=1，签发初始 TL
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

        // 1. 人机验证挑战 (/challenge/recaptcha) -> 服务端重算风控评分 (Interstitial cid=2) -> 更新 TL 推进至 /challenge/pwd (cid=3)
        switch (challengeType) {
            case "recaptcha" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "recaptcha");

                if (StringUtils.hasText(request.getCaptchaCode())) {
                    try {
                        CaptchaVerifyRequestDTO verifyDTO = new CaptchaVerifyRequestDTO();
                        verifyDTO.setCaptchaCode(request.getCaptchaCode());
                        verifyDTO.setAccount(identifier);
                        captchaSDK.verify(verifyDTO, identifier, CaptchaScene.LOGIN);
                    } catch (Exception ex) {
                        riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "人机身份验证未通过，请重试");
                    }
                }
                riskEvaluationEngine.reduceRisk(clientIp);

                // 内部流转经过 Interstitial (内部推进至 Step: cid=2) 并更新 TL 通关签名推进至密码挑战 (cid=3)
                String pwdTL = transitionLockManager.advanceTransitionTicket(
                        dsh, tl, cid, "recaptcha", 3, "pwd", identifier
                );

                return SignInFlowResponseVO.builder()
                        .dsh(dsh)
                        .tl(pwdTL)
                        .cid(3)
                        .nextStep("pwd")
                        .status("CHALLENGE")
                        .identifier(identifier)
                        .build();
            }


            // 2. 密码挑战 (/challenge/pwd)
            case "pwd" -> {
                transitionLockManager.validateTransitionTicket(dsh, tl, cid, "pwd");

                if (!StringUtils.hasText(request.getPassword())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入密码");
                }

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

                if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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

                // 维护模式校验
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

                // 密码核验成功，清理失败尝试记录
                redisTemplate.delete(attemptsKey);
                riskEvaluationEngine.recordSuccess(clientIp, CaptchaScene.LOGIN);
                riskEvaluationEngine.recordAccountSuccess(cleanIdentifier, CaptchaScene.LOGIN);

                // 二次验证 2FA 检查
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

                // 无需 2FA：全部核销完成，销毁 TL 与临时会话，签发正式 Token
                transitionLockManager.burnAndCompleteSession(dsh, tl, cid, "pwd");

                String jti = IdUtil.fastSimpleUUID();
                boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
                String accessToken = jwtUtil.generateAccessTokenFromUser(user, jti);
                String refreshToken = jwtUtil.generateRefreshTokenFromUser(user, jti, rememberMe);

                return SignInFlowResponseVO.builder()
                        .status("SUCCESS")
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .identifier(cleanIdentifier)
                        .build();
            }


            // 3. 二次验证 2FA 挑战 (/challenge/2fa 或 totp)
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

                return SignInFlowResponseVO.builder()
                        .status("SUCCESS")
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .identifier(identifier)
                        .build();
            }


            // 4. 设备环境确认挑战 (/challenge/device)
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

}
