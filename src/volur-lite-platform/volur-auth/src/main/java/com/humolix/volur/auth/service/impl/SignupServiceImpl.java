package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.domain.dto.signup.SignUpCompleteRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailResendRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEtotpChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpNameRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpPasswordRequestDTO;
import com.humolix.volur.auth.domain.vo.SignUpFlowResponseVO;
import com.humolix.volur.auth.domain.vo.SignUpOptionVO;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.service.SignupService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.framework.common.util.SpringContextHolder;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.event.UserStatusAction;
import com.humolix.volur.user.event.UserStatusChangedEvent;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final SystemSettingService systemSettingService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailService emailService;
    private final Sm3PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthCookieService authCookieService;
    private final FlowCryptoService flowCryptoService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public SignUpFlowResponseVO name(SignUpNameRequestDTO request) {
        if (!systemSettingService.getBoolean(SystemSettingKeys.AUTH_REGISTRATION_ENABLED, true)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "系统已关闭注册功能");
        }

        String clientIp = RequestUtil.getClientIp();
        RequestHeader requestHeader = RequestHeader.fromCurrentRequest();
        Session session = sessionManager.recoverySession(
                request.getDsh(), FlowNames.VLIF_WEB_SIGNUP.getFlowName(), FlowNames.FlowEntry.WEB_SIGN_UP.getEntry(), requestHeader, null
        );

        String rawUsername = request.getUsername();
        if (!StringUtils.hasText(rawUsername)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }

        String flowName = session.getFlow() != null ? session.getFlow().getFlowName() : FlowNames.VLIF_WEB_SIGNUP.getFlowName();
        String flowEntry = session.getFlow() != null ? session.getFlow().getFlowEntry() : FlowNames.FlowEntry.WEB_SIGN_UP.getEntry();

        String username = rawUsername.startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(rawUsername, flowName, flowEntry, session.getDsh())
                : rawUsername;

        if (username.length() < 3 || username.length() > 32 || !username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "用户名长度必须在3到32个字符之间，用户名只能包含字母、数字、下划线和连字符");
        }

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }

        if (session.getFlow() != null) {
            session.getFlow().setIdentifier(username);
        }
        sessionManager.setMetadata(session.getDsh(), "username", username);
        sessionManager.saveSession(session);

        boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true);
        String nextStep = requireEmailVerification ? "email" : "password";

        int currentCid = request.getCid() != null ? request.getCid() : 0;
        int nextCid = currentCid + 1;
        String currentStep = session.getFlow() != null && session.getFlow().getCurrentStep() != null ? session.getFlow().getCurrentStep() : "init";

        String nextTl = transitionLockManager.nextTransLock(
                session.getDsh(), request.getTl(), currentCid, currentStep, nextCid, nextStep, username
        );

        return SignUpFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTl)
                .cid(nextCid)
                .nextStep(nextStep)
                .status("CHALLENGE")
                .username(username)
                .build();
    }

    @Override
    public SignUpFlowResponseVO email(SignUpEmailRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "email");
        Session session = sessionManager.getSession(request.getDsh());

        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (!StringUtils.hasText(username) && session.getFlow() != null) {
            username = session.getFlow().getIdentifier();
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话用户名信息丢失，请重新开始注册");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该邮箱已被注册，请直接登录或更换邮箱");
        }

        String allowedSuffixes = systemSettingService.getSettingValue(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "");
        if (StringUtils.hasText(allowedSuffixes)) {
            String[] suffixes = allowedSuffixes.split(",");
            boolean matched = false;
            for (String s : suffixes) {
                String cleanSuffix = s.trim().toLowerCase();
                if (StringUtils.hasText(cleanSuffix) && email.endsWith(cleanSuffix)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "该邮箱后缀不符合平台注册要求");
            }
        }

        emailService.sendVerificationEmail(email, username);
        sessionManager.setMetadata(session.getDsh(), "email", email);

        int nextCid = request.getCid() + 1;
        String nextStep = "challenge/etotp";
        String nextTl = transitionLockManager.nextTransLock(
                session.getDsh(), request.getTl(), request.getCid(), "email", nextCid, nextStep, username
        );

        return SignUpFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTl)
                .cid(nextCid)
                .nextStep(nextStep)
                .status("CHALLENGE")
                .username(username)
                .email(email)
                .build();
    }

    @Override
    public void resendEmailCode(SignUpEmailResendRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "challenge/etotp");
        Session session = sessionManager.getSession(request.getDsh());
        String email = (String) sessionManager.getMetadata(session.getDsh(), "email");
        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未检测到关联邮箱，请重新输入邮箱");
        }
        emailService.sendVerificationEmail(email, StringUtils.hasText(username) ? username : "User");
    }

    @Override
    public SignUpFlowResponseVO verifyEtotp(SignUpEtotpChallengeRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "challenge/etotp");
        Session session = sessionManager.getSession(request.getDsh());
        String email = (String) sessionManager.getMetadata(session.getDsh(), "email");
        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话邮箱信息丢失，请重新开始注册");
        }

        boolean verified = emailService.verifyCode(email, request.getCode().trim());
        if (!verified) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱验证码错误或已失效");
        }

        sessionManager.setMetadata(session.getDsh(), "emailVerified", true);

        int nextCid = request.getCid() + 1;
        String nextStep = "password";
        String nextTl = transitionLockManager.nextTransLock(
                session.getDsh(), request.getTl(), request.getCid(), "challenge/etotp", nextCid, nextStep, username
        );

        return SignUpFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTl)
                .cid(nextCid)
                .nextStep(nextStep)
                .status("CHALLENGE")
                .username(username)
                .email(email)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignUpFlowResponseVO password(SignUpPasswordRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "password");
        Session session = sessionManager.getSession(request.getDsh());

        String rawPassword = request.getPassword();
        String rawConfirmPassword = request.getConfirmPassword();
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(rawConfirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }

        String flowName = session.getFlow() != null ? session.getFlow().getFlowName() : FlowNames.VLIF_WEB_SIGNUP.getFlowName();
        String flowEntry = session.getFlow() != null ? session.getFlow().getFlowEntry() : FlowNames.FlowEntry.WEB_SIGN_UP.getEntry();

        String plainPassword = rawPassword.startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(rawPassword, flowName, flowEntry, session.getDsh())
                : rawPassword;

        String plainConfirmPassword = rawConfirmPassword.startsWith("$V1$GCM$")
                ? flowCryptoService.decrypt(rawConfirmPassword, flowName, flowEntry, session.getDsh())
                : rawConfirmPassword;

        if (!Objects.equals(plainPassword, plainConfirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "两次输入的密码不一致");
        }

        if (plainPassword.length() < 6 || plainPassword.length() > 32 ||
                !plainPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间");
        }

        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        if (!StringUtils.hasText(username) && session.getFlow() != null) {
            username = session.getFlow().getIdentifier();
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "会话用户名信息丢失，请重新开始注册");
        }

        boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true);
        String email = (String) sessionManager.getMetadata(session.getDsh(), "email");
        Boolean emailVerified = (Boolean) sessionManager.getMetadata(session.getDsh(), "emailVerified");

        if (requireEmailVerification) {
            if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "尚未通过邮箱验证，无法设置密码完成注册");
            }
        }

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在，请重新开始");
        }
        if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "邮箱已被注册，请直接登录");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(plainPassword))
                .email(email)
                .role(UserRole.USER)
                .status(true)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名或邮箱已存在");
        }

        UserProfile userProfile = UserProfile.builder()
                .id(savedUser.getId())
                .isHidden(false)
                .build();
        userProfileRepository.save(userProfile);

        SpringContextHolder.publishEvent(new UserStatusChangedEvent(this, savedUser.getId(), UserStatusAction.REGISTER));

        String jti = IdUtil.fastSimpleUUID();
        String accessToken = jwtUtil.generateAccessTokenFromUser(savedUser, jti);
        String refreshToken = jwtUtil.generateRefreshTokenFromUser(savedUser, jti, false);

        authCookieService.writeAuthCookies(accessToken, refreshToken, false);

        sessionManager.setMetadata(session.getDsh(), "accessToken", accessToken);
        sessionManager.setMetadata(session.getDsh(), "refreshToken", refreshToken);
        sessionManager.setMetadata(session.getDsh(), "registeredUserId", savedUser.getId());

        int nextCid = request.getCid() + 1;
        String nextStep = "pt/options";
        String status = "COMPLETE";
        String nextTl = transitionLockManager.nextTransLock(
                session.getDsh(), request.getTl(), request.getCid(), "password", nextCid, nextStep, username
        );

        return SignUpFlowResponseVO.builder()
                .dsh(session.getDsh())
                .tl(nextTl)
                .cid(nextCid)
                .nextStep(nextStep)
                .status(status)
                .username(username)
                .email(email)
                .build();
    }

    @Override
    public List<SignUpOptionVO> getOptions(String dsh, String tl) {
        transitionLockManager.validateTransLock(dsh, tl, "pt/options");
        return List.of(
                SignUpOptionVO.builder()
                        .type("passkey")
                        .title("通行密钥")
                        .icon("passkey")
                        .available(true)
                        .build(),
                SignUpOptionVO.builder()
                        .type("totp")
                        .title("身份验证器")
                        .icon("shield")
                        .available(true)
                        .build()
        );
    }



    @Override
    public SignUpFlowResponseVO complete(SignUpCompleteRequestDTO request) {
        transitionLockManager.validateTransLock(request.getDsh(), request.getTl(), request.getCid(), "pt/options");
        Session session = sessionManager.getSession(request.getDsh());

        String accessToken = (String) sessionManager.getMetadata(session.getDsh(), "accessToken");
        String refreshToken = (String) sessionManager.getMetadata(session.getDsh(), "refreshToken");
        String username = (String) sessionManager.getMetadata(session.getDsh(), "username");
        String email = (String) sessionManager.getMetadata(session.getDsh(), "email");

        transitionLockManager.burnAndCompleteSession(request.getDsh(), request.getTl(), request.getCid(), "pt/options");

        if (StringUtils.hasText(accessToken) && StringUtils.hasText(refreshToken)) {
            authCookieService.writeAuthCookies(accessToken, refreshToken, false);
        }

        return SignUpFlowResponseVO.builder()
                .status("SUCCESS")
                .username(username)
                .email(email)
                .build();
    }

}
