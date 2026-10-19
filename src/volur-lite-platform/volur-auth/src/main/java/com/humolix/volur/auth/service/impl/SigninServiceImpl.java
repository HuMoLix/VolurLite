package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserRemoveRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserSelectRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInAuthenticateRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserItemVO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.risk.DeviceEnvironmentMatrixEvaluator;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.repository.UserTrustedDeviceRepository;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTrustedDeviceRepository userTrustedDeviceRepository;
    private final JwtUtil jwtUtil;
    private final RedisOps redisOps;
    private final StringRedisTemplate redisTemplate;
    private final AuthCookieService authCookieService;
    private final DeviceEnvironmentMatrixEvaluator deviceEnvironmentMatrixEvaluator;
    private final SessionDeviceBaselineValidator sessionDeviceBaselineValidator;

    private static final String VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX = "vredis:auth:identifier_failures:";
    private static final Duration IDENTIFIER_FAILURE_TTL = Duration.ofMinutes(15);
    private static final long IDENTIFIER_RECAPTCHA_THRESHOLD = 5L;

    @Override
    public AccountChooserProbeResponseVO probeAccounts(AccountChooserProbeRequestDTO request) {
        String currentUsername = SecurityUtils.getCurrentUsernameSafe();
        boolean authenticated = SecurityUtils.isAuthenticated();

        List<AccountChooserItemVO> list = new ArrayList<>();
        Set<String> processedUsernames = new HashSet<>();

        if (authenticated && StringUtils.hasText(currentUsername)) {
            User currentUser = userRepository.findByUsername(currentUsername.trim()).orElse(null);
            if (currentUser != null) {
                UserProfile profile = userProfileRepository.findById(currentUser.getId()).orElse(null);
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
                String displayName = StringUtils.hasText(currentUser.getRealName())
                        ? currentUser.getRealName().trim()
                        : currentUser.getUsername();
                list.add(AccountChooserItemVO.builder()
                        .identifier(currentUser.getUsername())
                        .username(currentUser.getUsername())
                        .email(currentUser.getEmail())
                        .displayName(displayName)
                        .avatarUrl(avatarUrl)
                        .sessionStatus(Boolean.TRUE.equals(currentUser.getStatus()) ? "ACTIVE" : "DISABLED")
                        .build());
                processedUsernames.add(currentUser.getUsername().toLowerCase());
            }
        }

        if (request != null && request.getIdentifiers() != null) {
            for (String rawIdentifier : request.getIdentifiers()) {
                if (!StringUtils.hasText(rawIdentifier)) {
                    continue;
                }
                String trimmed = rawIdentifier.trim();
                if (processedUsernames.contains(trimmed.toLowerCase())) {
                    continue;
                }

                User user = userRepository.findByUsername(trimmed)
                        .or(() -> userRepository.findByEmail(trimmed.toLowerCase()))
                        .orElse(null);

                if (user == null) {
                    list.add(AccountChooserItemVO.builder()
                            .identifier(trimmed)
                            .username(trimmed)
                            .displayName(trimmed)
                            .sessionStatus("SIGNED_OUT")
                            .build());
                    continue;
                }

                if (processedUsernames.contains(user.getUsername().toLowerCase())) {
                    continue;
                }

                String sessionStatus = "SIGNED_OUT";
                if (!Boolean.TRUE.equals(user.getStatus())) {
                    sessionStatus = "DISABLED";
                } else if (authenticated && user.getUsername().equalsIgnoreCase(currentUsername)) {
                    sessionStatus = "ACTIVE";
                }

                UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

                String displayName = StringUtils.hasText(user.getRealName())
                        ? user.getRealName().trim()
                        : user.getUsername();

                list.add(AccountChooserItemVO.builder()
                        .identifier(trimmed)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(displayName)
                        .avatarUrl(avatarUrl)
                        .sessionStatus(sessionStatus)
                        .build());
                processedUsernames.add(user.getUsername().toLowerCase());
            }
        }

        return AccountChooserProbeResponseVO.builder()
                .accounts(list)
                .build();
    }

    @Override
    public SignInFlowResponseVO selectAccount(AccountChooserSelectRequestDTO request) {
        SignInIdentifierRequestDTO identifierDTO = new SignInIdentifierRequestDTO();
        identifierDTO.setDsh(request.getDsh());
        identifierDTO.setIdentifier(request.getIdentifier());
        identifierDTO.setCid(0);
        return identifier(identifierDTO);
    }

    @Override
    public void removeAccount(AccountChooserRemoveRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getIdentifier())) {
            return;
        }
        String cleanIdentifier = request.getIdentifier().trim();
        User user = userRepository.findByUsername(cleanIdentifier)
                .or(() -> userRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElse(null);
        if (user != null) {
            String currentUsername = SecurityUtils.getCurrentUsernameSafe();
            if (user.getUsername().equalsIgnoreCase(currentUsername)) {
                authCookieService.clearAuthCookies();
            }
            String rawFp = RequestUtil.getDeviceFingerprint();
            if (StringUtils.hasText(rawFp)) {
                userTrustedDeviceRepository.findByUserIdAndDeviceHash(user.getId(), rawFp.trim())
                        .ifPresent(userTrustedDeviceRepository::delete);
            }
        }
    }

    @Override
    public SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request) {
        String clientIp = RequestUtil.getClientIp();
        RequestHeader requestHeader = RequestHeader.fromCurrentRequest();
        String rawFp = RequestUtil.getDeviceFingerprint();
        String identifier = request.getIdentifier();
        String subjectKey = buildClientSubjectKey(clientIp, rawFp);

        Session session = sessionManager.recoverySession(
                request.getDsh(), FlowNames.VLIF_WEB_LOGIN.getFlowName(), FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry(), requestHeader, null
        );
        SigninFlow flow = SigninFlow.from(session.getFlow());
        flow.setIdentifier(identifier);
        session.setFlow(flow);

        long failures = getIdentifierFailures(subjectKey);

        RiskLevel riskLevel = riskEvaluationEngine.evaluateComprehensiveRisk(
                clientIp, identifier, rawFp, CaptchaScene.LOGIN
        );
        RiskLevel envRisk = deviceEnvironmentMatrixEvaluator.evaluate(
                session.getRequestDeviceParams(),
                session.getRequestHeader() != null ? session.getRequestHeader() : requestHeader
        );
        RiskLevel finalRisk = envRisk.ordinal() > riskLevel.ordinal() ? envRisk : riskLevel;
        session.setRiskLevel(finalRisk.name());
        sessionManager.saveSession(session);

        boolean isEnvSuspicious = finalRisk == RiskLevel.HIGH;

        if (failures >= IDENTIFIER_RECAPTCHA_THRESHOLD || isEnvSuspicious) {
            flow.addWaitingStepFirst(SigninFlow.STEP_RECAPTCHA);
            sessionManager.saveSession(session);
            String tl = transitionLockManager.nextTransLock(
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

        String tl = transitionLockManager.nextTransLock(
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
    public SignInFlowResponseVO authenticate(SignInAuthenticateRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid();

        transitionLockManager.validateTransLock(dsh, tl, cid, SigninFlow.STEP_AUTHENTICATE);

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        SigninFlow flow = SigninFlow.from(session.getFlow());
        flow.completeStep(SigninFlow.STEP_AUTHENTICATE);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被封禁");
        }

        transitionLockManager.burnAndCompleteSession(dsh, tl, cid, SigninFlow.STEP_AUTHENTICATE);

        String jti = IdUtil.fastSimpleUUID();
        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        String accessToken = jwtUtil.generateAccessTokenFromUser(user, jti);
        String refreshToken = jwtUtil.generateRefreshTokenFromUser(user, jti, rememberMe);

        authCookieService.writeAuthCookies(accessToken, refreshToken, rememberMe);

        UserProfile profile = user.getId() != null ? userProfileRepository.findById(user.getId()).orElse(null) : null;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        String displayName = StringUtils.hasText(user.getRealName())
                ? user.getRealName().trim()
                : user.getUsername();

        return SignInFlowResponseVO.builder()
                .status("SUCCESS")
                .identifier(identifier)
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .build();
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

}
