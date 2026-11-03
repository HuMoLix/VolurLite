package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.ChallengeSelectRequestDTO;
import com.humolix.volur.security.domain.dto.EmailOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.ReCAPTCHAChallengeSubmitDTO;
import com.humolix.volur.security.domain.dto.SMSOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.ChallengeOptionVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.ChallengeService;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.PasskeyService;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.security.util.MaskUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private final UserTrustedDeviceService userTrustedDeviceService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RecaptchaService recaptchaService;
    private final FlowCryptoService flowCryptoService;
    private final PasskeyService passkeyService;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final SystemSettingService systemSettingService;
    private final StringRedisTemplate redisTemplate;

    private static final String VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX = "vredis:auth:identifier:failures:";


    @Override
    public FlowResponseVO verifyEmailOTP(EmailOTPChallengeRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        transitionLockManager.validateTransLock(dsh, tl, "etotp");

        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        String clientIp = RequestUtil.getClientIp();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未检测到关联邮箱");
        }

        if (!emailService.verifyCode(user.getEmail(), request.getCode().trim())) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱安全验证码错误或已过期");
        }

        if (session.getFlow() != null) {
            session.getFlow().completeStep("etotp");
            sessionManager.saveSession(session);
        }

        riskEvaluationEngine.recordSuccess(clientIp, CaptchaScene.LOGIN);

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

        int nextCid = cid + 1;
        String authTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, "etotp", nextCid, SigninFlow.STEP_AUTHENTICATE, identifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(authTL)
                .cid(nextCid)
                .nextStep(SigninFlow.STEP_AUTHENTICATE)
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public void sendEmailOTP(String dsh, String tl) {
        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        transitionLockManager.validateTransLock(dsh, tl, "etotp");

        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未检测到关联邮箱");
        }

        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), true);
    }

    @Override
    public FlowResponseVO verifySMSOTP(SMSOTPChallengeRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);

        String currentStep = session.getFlow() != null ? session.getFlow().getCurrentStep() : "smsotp";
        transitionLockManager.validateTransLock(dsh, tl, currentStep);

        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (session.getFlow() != null) {
            session.getFlow().completeStep(currentStep);
            sessionManager.saveSession(session);
        }

        int nextCid = cid + 1;
        String authTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, currentStep, nextCid, SigninFlow.STEP_AUTHENTICATE, identifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(authTL)
                .cid(nextCid)
                .nextStep(SigninFlow.STEP_AUTHENTICATE)
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public FlowResponseVO verifyReCAPTCHA(ReCAPTCHAChallengeSubmitDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        transitionLockManager.validateTransLock(dsh, tl, "recaptcha");

        String clientIp = RequestUtil.getClientIp();
        if (!recaptchaService.validateAndBurnTicket(dsh, request.getRecaptchaTicket())) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机安全验证未通过或凭据已失效，请重试");
        }

        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        String recoveryType = StringUtils.hasText(request.getRecoveryType())
                ? request.getRecoveryType().trim()
                : (String) sessionManager.getMetadata(dsh, "recoveryType");

        if ("password".equalsIgnoreCase(recoveryType)) {
            riskEvaluationEngine.reduceRisk(clientIp);
            int nextCid = cid + 1;
            String nextStep = "emailotp";
            String nextTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, "recaptcha", nextCid, nextStep, identifier
            );
            if (session.getFlow() != null) {
                SigninFlow flow = SigninFlow.from(session.getFlow());
                flow.setActiveTL(nextTL);
                flow.setCid(nextCid);
                flow.setCurrentStep(nextStep);
                session.setFlow(flow);
                sessionManager.saveSession(session);
            }
            return FlowResponseVO.builder()
                    .dsh(dsh)
                    .tl(nextTL)
                    .cid(nextCid)
                    .nextStep(nextStep)
                    .status("CHALLENGE")
                    .identifier(identifier)
                    .build();
        }

        if ("username".equalsIgnoreCase(recoveryType) || !StringUtils.hasText(identifier)) {
            riskEvaluationEngine.reduceRisk(clientIp);
            int nextCid = cid + 1;
            String nextTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, "recaptcha", nextCid, "email", null
            );
            if (session.getFlow() != null) {
                SigninFlow flow = SigninFlow.from(session.getFlow());
                flow.setActiveTL(nextTL);
                flow.setCid(nextCid);
                flow.setCurrentStep("email");
                session.setFlow(flow);
                sessionManager.saveSession(session);
            }
            return FlowResponseVO.builder()
                    .dsh(dsh)
                    .tl(nextTL)
                    .cid(nextCid)
                    .nextStep("email")
                    .status("CHALLENGE")
                    .build();
        }

        String pendingChallenge = (String) sessionManager.getMetadata(dsh, "pendingChallenge");
        if (!StringUtils.hasText(pendingChallenge) && session.getMetadata() != null) {
            pendingChallenge = (String) session.getMetadata().get("pendingChallenge");
        }
        if (!StringUtils.hasText(pendingChallenge) && session.getFlow() != null && session.getFlow().getWaitingSteps() != null) {
            List<String> waiting = session.getFlow().getWaitingSteps();
            int idx = waiting.indexOf("recaptcha");
            if (idx >= 0 && idx + 1 < waiting.size()) {
                String candidate = waiting.get(idx + 1);
                if ("etotp".equalsIgnoreCase(candidate) || "smsotp".equalsIgnoreCase(candidate) || "totp".equalsIgnoreCase(candidate)) {
                    pendingChallenge = candidate;
                }
            }
        }

        if (StringUtils.hasText(pendingChallenge)) {
            sessionManager.removeMetadata(dsh, "pendingChallenge");
            if (session.getMetadata() != null) {
                session.getMetadata().remove("pendingChallenge");
            }
            riskEvaluationEngine.reduceRisk(clientIp);

            User user = userRepository.findByUsername(identifier)
                    .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在"));

            String normalizedStep = pendingChallenge.trim().toLowerCase();
            if ("etotp".equals(normalizedStep) || "emailotp".equals(normalizedStep)) {
                if (!StringUtils.hasText(user.getEmail())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "未检测到关联邮箱");
                }
                emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), true);
                normalizedStep = "etotp";
            } else if ("smsotp".equals(normalizedStep) || "sms".equals(normalizedStep)) {
                if (!StringUtils.hasText(user.getPhone())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "未检测到关联手机号");
                }
                normalizedStep = "smsotp";
            } else if ("totp".equals(normalizedStep)) {
                if (!Boolean.TRUE.equals(user.getIsTotpEnabled()) || !StringUtils.hasText(user.getTotpSecret())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "用户未配置动态身份验证器");
                }
                normalizedStep = "totp";
            }

            int nextCid = cid + 1;
            String nextTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, "recaptcha", nextCid, normalizedStep, identifier
            );
            if (session.getFlow() != null) {
                SigninFlow flow = SigninFlow.from(session.getFlow());
                flow.setWaitingSteps(new ArrayList<>(List.of(normalizedStep, SigninFlow.STEP_AUTHENTICATE)));
                flow.setActiveTL(nextTL);
                flow.setCid(nextCid);
                flow.setCurrentStep(normalizedStep);
                session.setFlow(flow);
                sessionManager.saveSession(session);
            }

            return FlowResponseVO.builder()
                    .dsh(dsh)
                    .tl(nextTL)
                    .cid(nextCid)
                    .nextStep(normalizedStep)
                    .status("CHALLENGE")
                    .identifier(identifier)
                    .build();
        }

        String deviceFp = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getFingerprint() : null;
        String subjectKey = buildClientSubjectKey(clientIp, deviceFp);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElse(null);

        if (user == null) {
            recordIdentifierFailure(subjectKey);
            int nextCid = cid + 1;
            String nextTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, "recaptcha", nextCid, "identifier", identifier
            );
            return FlowResponseVO.builder()
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
        String pwdTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, "recaptcha", nextCid, "pwd", identifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(pwdTL)
                .cid(nextCid)
                .nextStep("pwd")
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public FlowResponseVO selectChallenge(ChallengeSelectRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;
        String targetType = request.getTargetChallengeType().trim().toLowerCase();

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        SigninFlow flow = SigninFlow.from(session.getFlow());
        String currentStep = flow.getCurrentStep();
        transitionLockManager.validateTransLock(dsh, tl, currentStep);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在"));

        switch (targetType) {
            case "etotp" -> {
                if (!StringUtils.hasText(user.getEmail())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定安全邮箱，无法使用邮箱验证码登录");
                }
            }
            case "smsotp", "sms" -> {
                if (!StringUtils.hasText(user.getPhone())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定手机号，无法使用短信验证码登录");
                }
            }
            case "totp" -> {
                if (!Boolean.TRUE.equals(user.getIsTotpEnabled()) || !StringUtils.hasText(user.getTotpSecret())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定动态身份验证器，无法使用此方式验证");
                }
            }
            case "pk", "device" -> {
                if (!passkeyService.hasPasskey(user.getId())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定通行密钥，无法使用此方式验证");
                }
            }
            case "pwd", "atcr", "backup_code" -> {
            }
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的安全挑战类型: " + targetType);
        }

        boolean requireRecaptcha = "etotp".equalsIgnoreCase(targetType)
                || "smsotp".equalsIgnoreCase(targetType)
                || "sms".equalsIgnoreCase(targetType);

        if (requireRecaptcha) {
            String pendingStep = ("smsotp".equalsIgnoreCase(targetType) || "sms".equalsIgnoreCase(targetType))
                    ? "smsotp"
                    : "etotp";
            if (session.getMetadata() == null) {
                session.setMetadata(new HashMap<>());
            }
            session.getMetadata().put("pendingChallenge", pendingStep);
            sessionManager.setMetadata(dsh, "pendingChallenge", pendingStep);
            flow.setWaitingSteps(new ArrayList<>(List.of("recaptcha", pendingStep, SigninFlow.STEP_AUTHENTICATE)));
            int nextCid = cid;
            String nextTL = transitionLockManager.nextTransLock(
                    dsh, tl, cid, currentStep, nextCid, "recaptcha", identifier
            );
            flow.setActiveTL(nextTL);
            flow.setCid(nextCid);
            flow.setCurrentStep("recaptcha");
            session.setFlow(flow);
            sessionManager.saveSession(session);

            return FlowResponseVO.builder()
                    .dsh(dsh)
                    .tl(nextTL)
                    .cid(nextCid)
                    .nextStep("recaptcha")
                    .status("CHALLENGE")
                    .identifier(identifier)
                    .build();
        }

        flow.setWaitingSteps(new ArrayList<>(List.of(targetType, SigninFlow.STEP_AUTHENTICATE)));
        session.setFlow(flow);
        sessionManager.saveSession(session);

        int nextCid = cid;
        String nextTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, currentStep, nextCid, targetType, identifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(nextTL)
                .cid(nextCid)
                .nextStep(targetType)
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public List<ChallengeOptionVO> getChallengeSelection(String dsh, String currentType) {
        String cleanCurrent = StringUtils.hasText(currentType) ? currentType.trim().toLowerCase() : "";

        String maskedEmail = null;
        String maskedPhone = null;
        boolean hasEmail = false;
        boolean hasPhone = false;
        boolean hasPasskey = false;
        boolean hasTotp = false;

        if (StringUtils.hasText(dsh)) {
            try {
                Session session = sessionManager.getSession(dsh);
                if (session != null && session.getFlow() != null && StringUtils.hasText(session.getFlow().getIdentifier())) {
                    String identifier = session.getFlow().getIdentifier().trim();
                    User user = userRepository.findByUsername(identifier)
                            .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                            .orElse(null);
                    if (user != null) {
                        if (StringUtils.hasText(user.getEmail())) {
                            maskedEmail = MaskUtil.emailMask(user.getEmail());
                            hasEmail = true;
                        }
                        if (StringUtils.hasText(user.getPhone())) {
                            maskedPhone = MaskUtil.phoneMask(user.getPhone());
                            hasPhone = true;
                        }
                        if (user.getId() != null) {
                            hasPasskey = passkeyService.hasPasskey(user.getId());
                        }
                        if (Boolean.TRUE.equals(user.getIsTotpEnabled()) && StringUtils.hasText(user.getTotpSecret())) {
                            hasTotp = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<ChallengeOptionVO> allOptions = new ArrayList<>();

        if (hasEmail) {
            allOptions.add(ChallengeOptionVO.builder()
                    .type("etotp")
                    .title("获取电子邮件验证码")
                    .subtitle("向安全邮箱 " + maskedEmail + " 发送一次性安全代码")
                    .icon("email")
                    .path("/v1/signin/challenge/etotp")
                    .build());
        }

        if (hasPhone) {
            allOptions.add(ChallengeOptionVO.builder()
                    .type("smsotp")
                    .title("在手机上获取动态验证码")
                    .subtitle("向安全手机 " + maskedPhone + " 发送短信验证码")
                    .icon("sms")
                    .path("/v1/signin/challenge/smsotp")
                    .build());
        }

        if (hasPasskey) {
            allOptions.add(ChallengeOptionVO.builder()
                    .type("pk")
                    .title("使用安全密钥或通行密钥")
                    .subtitle("通过 FIDO2 / WebAuthn 设备或生物识别验证")
                    .icon("device")
                    .path("/v1/signin/challenge/pk")
                    .build());
        }

        if (hasTotp) {
            allOptions.add(ChallengeOptionVO.builder()
                    .type("totp")
                    .title("使用身份验证器动态口令")
                    .subtitle("输入验证器应用生成的 6 位动态验证码")
                    .icon("shield")
                    .path("/v1/signin/challenge/totp")
                    .build());
        }

        allOptions.add(ChallengeOptionVO.builder()
                .type("pwd")
                .title("输入您的密码")
                .subtitle("使用您之前设定的账号密码进行验证")
                .icon("key")
                .path("/v1/signin/challenge/pwd")
                .build());

        return allOptions.stream()
                .filter(opt -> !opt.getType().equalsIgnoreCase(cleanCurrent))
                .toList();
    }

    private String buildClientSubjectKey(String clientIp, String deviceFp) {
        if (StringUtils.hasText(deviceFp)) {
            return clientIp + ":" + deviceFp;
        }
        return clientIp;
    }

    private void recordIdentifierFailure(String subjectKey) {
        String key = VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX + subjectKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }
    }

    private void resetIdentifierFailures(String subjectKey) {
        String key = VREDIS_AUTH_IDENTIFIER_FAILURES_KEY_PREFIX + subjectKey;
        redisTemplate.delete(key);
    }

}
