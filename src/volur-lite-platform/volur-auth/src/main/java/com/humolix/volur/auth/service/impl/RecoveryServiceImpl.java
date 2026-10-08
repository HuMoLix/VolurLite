package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryEmailOtpRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryResetRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoveryEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.vo.PasswordRecoveryOptionsVO;
import com.humolix.volur.auth.domain.vo.RecoveryFlowResponseVO;
import com.humolix.volur.auth.domain.vo.RecoveryMethodVO;
import com.humolix.volur.auth.domain.vo.UsernameRecoveryOptionsVO;
import com.humolix.volur.auth.service.RecoveryService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryServiceImpl implements RecoveryService {

    private final SystemSettingService systemSettingService;
    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FlowCryptoService flowCryptoService;

    @Override
    public UsernameRecoveryOptionsVO getUsernameRecoveryOptions() {
        boolean mailConfigured = StringUtils.hasText(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));
        boolean emailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false) && mailConfigured;
        boolean phoneEnabled = systemSettingService.getBoolean(SystemSettingKeys.SMS_ENABLED, false);

        List<RecoveryMethodVO> methods = new ArrayList<>();
        if (emailEnabled) {
            methods.add(RecoveryMethodVO.builder()
                    .method("EMAIL")
                    .name("电子邮箱")
                    .description("通过绑定的安全邮箱找回您的用户名")
                    .icon("mail")
                    .build());
        }
        if (phoneEnabled) {
            methods.add(RecoveryMethodVO.builder()
                    .method("PHONE")
                    .name("手机短信")
                    .description("通过绑定的安全手机号找回您的用户名")
                    .icon("phone")
                    .build());
        }

        return UsernameRecoveryOptionsVO.builder()
                .methods(methods)
                .build();
    }

    @Override
    public RecoveryFlowResponseVO selectRecoveryMethod(UsernameRecoverySelectRequestDTO request) {
        String method = request.getMethod().toUpperCase();
        if ("EMAIL".equals(method)) {
            boolean mailConfigured = StringUtils.hasText(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));
            boolean emailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false) && mailConfigured;
            if (!emailEnabled) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务未配置或未开启，无法通过邮箱找回");
            }
        } else if ("PHONE".equals(method)) {
            boolean phoneEnabled = systemSettingService.getBoolean(SystemSettingKeys.SMS_ENABLED, false);
            if (!phoneEnabled) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "短信服务未开启，无法通过手机找回");
            }
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的找回方式: " + request.getMethod());
        }

        Session session = sessionManager.recoverySession(
                request.getDsh(),
                FlowNames.VLIF_WEB_LOGIN.getFlowName(),
                FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry(),
                RequestHeader.fromCurrentRequest(),
                null
        );
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put("recoveryType", "username");
        session.getMetadata().put("recoveryMethod", method);

        SigninFlow flow = SigninFlow.from(session.getFlow());
        int currentCid = request.getCid() != null ? request.getCid() : 0;
        int nextCid = currentCid + 1;
        String nextTL;
        if (StringUtils.hasText(request.getTl()) && StringUtils.hasText(flow.getActiveTL())) {
            nextTL = transitionLockManager.nextTransLock(
                    session.getDsh(), request.getTl(), currentCid, flow.getCurrentStep(), nextCid, "recaptcha", null
            );
        } else {
            nextTL = transitionLockManager.createTransLock(session.getDsh(), nextCid, "recaptcha", null);
        }
        flow.setActiveTL(nextTL);
        flow.setCid(nextCid);
        flow.setCurrentStep("recaptcha");
        session.setFlow(flow);
        sessionManager.saveSession(session);

        return RecoveryFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTL)
                .cid(nextCid)
                .nextStep("recaptcha")
                .status("CHALLENGE")
                .build();
    }

    @Override
    public RecoveryFlowResponseVO submitRecoveryEmail(UsernameRecoveryEmailRequestDTO request) {
        boolean mailConfigured = StringUtils.hasText(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));
        boolean emailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false) && mailConfigured;
        if (!emailEnabled) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务未配置或未开启，无法通过邮箱找回");
        }

        int currentCid = request.getCid() != null ? request.getCid() : 1;
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), "email");

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.info("[Recovery] 邮箱匹配成功，准备投递找回用户名邮件: email={}, username={}", normalizedEmail, user.getUsername());
            emailService.sendRecoverUsernameEmail(normalizedEmail, user.getUsername());
        } else {
            log.warn("[Recovery] 邮箱未匹配到任何已注册用户: email={}", normalizedEmail);
        }

        transitionLockManager.burnAndCompleteSession(request.getDsh(), request.getTl(), currentCid, "email");

        return RecoveryFlowResponseVO.builder()
                .dsh(request.getDsh())
                .status("SUCCESS")
                .build();
    }

    @Override
    public PasswordRecoveryOptionsVO getPasswordRecoveryOptions(String dsh) {
        if (!StringUtils.hasText(dsh)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话句柄 (dsh) 不能为空");
        }
        Session session = sessionManager.getSession(dsh);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean mailConfigured = StringUtils.hasText(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));
        boolean emailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false) && mailConfigured;
        boolean phoneEnabled = systemSettingService.getBoolean(SystemSettingKeys.SMS_ENABLED, false);

        List<RecoveryMethodVO> methods = new ArrayList<>();
        String maskedEmail = null;
        if (StringUtils.hasText(user.getEmail())) {
            maskedEmail = maskEmail(user.getEmail());
            if (emailEnabled) {
                methods.add(RecoveryMethodVO.builder()
                        .method("EMAIL")
                        .name("安全邮箱")
                        .description("向您的安全邮箱 " + maskedEmail + " 发送验证码")
                        .icon("mail")
                        .build());
            }
        }

        String maskedPhone = null;
        if (StringUtils.hasText(user.getPhone())) {
            maskedPhone = maskPhone(user.getPhone());
            if (phoneEnabled) {
                methods.add(RecoveryMethodVO.builder()
                        .method("PHONE")
                        .name("安全手机")
                        .description("向您的安全手机 " + maskedPhone + " 发送验证码")
                        .icon("phone")
                        .build());
            }
        }

        return PasswordRecoveryOptionsVO.builder()
                .methods(methods)
                .maskedEmail(maskedEmail)
                .maskedPhone(maskedPhone)
                .build();
    }

    @Override
    public RecoveryFlowResponseVO selectPasswordRecoveryMethod(PasswordRecoverySelectRequestDTO request) {
        String method = request.getMethod().toUpperCase();
        if ("EMAIL".equals(method)) {
            boolean mailConfigured = StringUtils.hasText(systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS));
            boolean emailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false) && mailConfigured;
            if (!emailEnabled) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务未配置或未开启，无法通过邮箱找回密码");
            }
        } else if ("PHONE".equals(method)) {
            boolean phoneEnabled = systemSettingService.getBoolean(SystemSettingKeys.SMS_ENABLED, false);
            if (!phoneEnabled) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "短信服务未开启，无法通过手机找回密码");
            }
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的找回密码验证方式: " + request.getMethod());
        }

        Session session = sessionManager.recoverySession(
                request.getDsh(),
                FlowNames.VLIF_WEB_LOGIN.getFlowName(),
                FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry(),
                RequestHeader.fromCurrentRequest(),
                null
        );
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新输入账号");
        }

        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put("recoveryType", "password");
        session.getMetadata().put("recoveryMethod", method);

        SigninFlow flow = SigninFlow.from(session.getFlow());
        int currentCid = request.getCid() != null ? request.getCid() : 0;
        int nextCid = currentCid + 1;
        String nextTL;
        if (StringUtils.hasText(request.getTl()) && StringUtils.hasText(flow.getActiveTL())) {
            nextTL = transitionLockManager.nextTransLock(
                    session.getDsh(), request.getTl(), currentCid, flow.getCurrentStep(), nextCid, "recaptcha", identifier
            );
        } else {
            nextTL = transitionLockManager.createTransLock(session.getDsh(), nextCid, "recaptcha", identifier);
        }
        flow.setActiveTL(nextTL);
        flow.setCid(nextCid);
        flow.setCurrentStep("recaptcha");
        session.setFlow(flow);
        sessionManager.saveSession(session);

        return RecoveryFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTL)
                .cid(nextCid)
                .nextStep("recaptcha")
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public void sendPasswordRecoveryEmailOtp(String dsh, String tl) {
        if (!StringUtils.hasText(dsh)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话句柄不能为空");
        }
        Session session = sessionManager.getSession(dsh);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失");
        }
        transitionLockManager.validateTransLock(dsh, tl, "emailotp");

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定安全邮箱");
        }

        emailService.sendChangePasswordEmail(user.getEmail(), user.getUsername());
    }

    @Override
    public RecoveryFlowResponseVO verifyPasswordRecoveryEmailOtp(PasswordRecoveryEmailOtpRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 2;

        Session session = sessionManager.getSession(dsh);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失");
        }

        transitionLockManager.validateTransLock(dsh, tl, "emailotp");

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定安全邮箱");
        }

        if (!emailService.verifyCode(user.getEmail(), request.getCode().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱安全验证码错误或已过期");
        }

        int nextCid = cid + 1;
        String nextTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, "emailotp", nextCid, "reset", identifier
        );
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put("emailVerified", true);
        SigninFlow flow = SigninFlow.from(session.getFlow());
        flow.setActiveTL(nextTL);
        flow.setCid(nextCid);
        flow.setCurrentStep("reset");
        session.setFlow(flow);
        sessionManager.saveSession(session);
        sessionManager.setMetadata(dsh, "emailVerified", true);

        return RecoveryFlowResponseVO.builder()
                .dsh(dsh)
                .tl(nextTL)
                .cid(nextCid)
                .nextStep("reset")
                .status("CHALLENGE")
                .identifier(identifier)
                .build();
    }

    @Override
    public RecoveryFlowResponseVO submitPasswordReset(PasswordRecoveryResetRequestDTO request) {
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 3;

        Session session = sessionManager.getSession(dsh);
        String identifier = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话账号信息丢失，请重新开始");
        }

        transitionLockManager.validateTransLock(dsh, tl, "reset");

        Boolean emailVerified = (Boolean) sessionManager.getMetadata(dsh, "emailVerified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "尚未通过身份验证，无法重置密码");
        }

        String flowName = session.getFlow() != null ? session.getFlow().getFlowName() : FlowNames.VLIF_WEB_LOGIN.getFlowName();
        String flowEntry = session.getFlow() != null ? session.getFlow().getFlowEntry() : FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry();

        String rawPassword = request.getPassword().trim();
        String rawConfirmPassword = request.getConfirmPassword().trim();

        String plainPassword = rawPassword.startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(rawPassword, flowName, flowEntry, dsh)
                : rawPassword;

        String plainConfirmPassword = rawConfirmPassword.startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(rawConfirmPassword, flowName, flowEntry, dsh)
                : rawConfirmPassword;

        if (!Objects.equals(plainPassword, plainConfirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "两次输入的密码不一致");
        }

        if (plainPassword.length() < 6 || plainPassword.length() > 32 ||
                !plainPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setPassword(passwordEncoder.encode(plainPassword));
        userRepository.save(user);

        transitionLockManager.burnAndCompleteSession(dsh, tl, cid, "reset");

        log.info("[Recovery] 用户密码重置成功: identifier={}", identifier);

        return RecoveryFlowResponseVO.builder()
                .dsh(dsh)
                .status("SUCCESS")
                .identifier(identifier)
                .build();
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) {
            return email;
        }
        String prefix = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (prefix.length() <= 2) {
            return prefix.charAt(0) + "***" + domain;
        }
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + domain;
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return phone;
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }

}
