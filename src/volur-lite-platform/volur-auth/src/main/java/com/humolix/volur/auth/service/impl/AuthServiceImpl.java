package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.auth.exception.InvalidCaptchaException;
import com.humolix.volur.auth.exception.TokenAlreadyBlacklistedException;
import com.humolix.volur.auth.exception.TokenInvalidException;
import com.humolix.volur.auth.exception.UserOrEmailExistException;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenMissingException;
import com.humolix.volur.framework.common.util.SpringContextHolder;
import com.humolix.volur.notification.domain.enums.SecurityEventType;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.notification.event.SecurityNotificationEvent;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.event.UserStatusAction;
import com.humolix.volur.user.event.UserStatusChangedEvent;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import com.humolix.volur.verification.service.EmailService;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final Sm3PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final CaptchaSDK captchaSDK;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SystemSettingService systemSettingService;
    private final VolurCacheTemplate cacheTemplate;
    private final RedisOps redisOps;
    private final StringRedisTemplate redisTemplate;

    private static final String VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX = "vredis:auth:login_attempts:";
    private static final String VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX = "vredis:auth:login_lock:";
    private static final String VREDIS_AUTH_PASSWORD_RESET_KEY_PREFIX = "vredis:verification:password_reset:";
    private static final String VREDIS_AUTH_CHANGE_PASSWORD_KEY_PREFIX = "vredis:auth:change_password:";

    private void evictUserCache(User user) {
        if (user != null) {
            evictUserCache(user.getUsername(), user.getEmail());
        }
    }

    private void evictUserCache(String username, String email) {
        if (StringUtils.hasText(username)) {
            String u = username.trim();
            cacheTemplate.evict(CacheNames.USER_DETAILS, u);
            cacheTemplate.evict(CacheNames.USER_DETAILS, u.toLowerCase());
        }
        if (StringUtils.hasText(email)) {
            String e = email.trim();
            cacheTemplate.evict(CacheNames.USER_DETAILS, e);
            cacheTemplate.evict(CacheNames.USER_DETAILS, e.toLowerCase());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequestDTO request) {
        if (!systemSettingService.getBoolean(SystemSettingKeys.AUTH_REGISTRATION_ENABLED, true)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "系统已关闭注册功能");
        }

        String username = request.getUsername();
        String email = request.getEmail();

        if (userRepository.existsByUsername(username) || (StringUtils.hasText(email) && userRepository.existsByEmail(email))) {
            throw new UserOrEmailExistException();
        }

        boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true);

        if (requireEmailVerification) {
            if (!StringUtils.hasText(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
            }
            String emailCaptcha = request.getEmailCaptcha();
            if (!StringUtils.hasText(emailCaptcha)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱验证码不能为空");
            }
            if (!emailService.verifyCode(email, emailCaptcha)) {
                throw new InvalidCaptchaException("邮箱验证码错误或已过期");
            }
        } else {
            captchaSDK.verify(request.getCaptcha(), username, CaptchaScene.REGISTER);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .role(UserRole.USER)
                .status(true)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserOrEmailExistException();
        }

        UserProfile userProfile = UserProfile.builder()
                .id(savedUser.getId())
                .isHidden(false)
                .build();

        userProfileRepository.save(userProfile);

        SpringContextHolder.publishEvent(new UserStatusChangedEvent(this, savedUser.getId(), UserStatusAction.REGISTER));
    }

    @Override
    public AuthenticateResponseVO authenticate(AuthenticateRequestDTO request) {
        String username = request.getUsername();
        String lusername = username.toLowerCase();

        captchaSDK.verify(request.getCaptcha(), username, CaptchaScene.LOGIN);

        int maxAttempts = systemSettingService.getInt("system.auth.max_login_attempts", 5);
        String lockKey = VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX + lusername;
        String attemptsKey = VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX + lusername;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
        }

        try {
            Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                    lusername, request.getPassword()
            );
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            boolean isMaintenance = systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false);
            if (isMaintenance) {
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                                || "ROLE_SUPER_ADMIN".equals(a.getAuthority())
                                || "ADMIN".equals(a.getAuthority())
                                || "SUPER_ADMIN".equals(a.getAuthority()));
                if (!isAdmin) {
                    String notice = systemSettingService.getSettingValue(
                            SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE,
                            "系统正在全站维护升级中，暂仅限管理员登录"
                    );
                    throw new MaintenanceModeException(notice);
                }
            }

            String jti = IdUtil.fastSimpleUUID();

            boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
            String accessToken = jwtUtil.generateAccessToken(authentication, jti);
            String refreshToken = jwtUtil.generateRefreshToken(authentication, jti, rememberMe);

            // 2. 认证成功：通知 CaptchaSDK 削减风控计数并清理尝试记录
            captchaSDK.recordSuccess(username, CaptchaScene.LOGIN);
            redisTemplate.delete(attemptsKey);

            return AuthenticateResponseVO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (AuthenticationException ex) {
            // 3. 认证失败：通知 CaptchaSDK 累加失败计数并自适应跃迁风控
            captchaSDK.recordFailure(username, CaptchaScene.LOGIN);

            long attempts = redisOps.incr(attemptsKey, 1, Duration.ofMinutes(30));
            if (attempts >= maxAttempts) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(15));
                redisTemplate.delete(attemptsKey);
                throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
            } else {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
            }
        }
    }

    @Override
    public void processForgotPassword(ForgotPasswordRequestDTO request) {
        captchaSDK.verify(request.getCaptcha(), request.getUsername(), CaptchaScene.RESET_PASSWORD);
        processForgotPassword(request.getEmail(), request.getUsername());
    }

    @Override
    public void processForgotPassword(String email, String username) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedUsername = username.trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent() && userOpt.get().getUsername().equals(normalizedUsername)) {
            emailService.sendResetPasswordEmail(normalizedEmail, normalizedUsername);
        } else {
            log.warn("[Volur-Auth] 忘记密码请求的用户或邮箱不匹配 -> 用户名: {}, 邮箱: {}", normalizedUsername, normalizedEmail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByToken(String token, String newPassword) {
        String redisKey = VREDIS_AUTH_PASSWORD_RESET_KEY_PREFIX + token.trim();
        String email = redisTemplate.opsForValue().get(redisKey);
        if (email == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "链接已过期或无效");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(redisKey);
        tokenService.forceLogoutUser(user.getId());
        evictUserCache(user);

        SpringContextHolder.publishEvent(new SecurityNotificationEvent(
                this,
                user.getId(),
                SecurityEventType.PASSWORD_RESET,
                RequestUtil.getClientIp(),
                "通过重置令牌重置密码"
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequestDTO request) {
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "绑定的用户不存在"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        tokenService.forceLogoutUser(user.getId());
        evictUserCache(user);

        SpringContextHolder.publishEvent(new SecurityNotificationEvent(
                this,
                user.getId(),
                SecurityEventType.PASSWORD_RESET,
                RequestUtil.getClientIp(),
                "通过邮箱验证码重置密码"
        ));
    }

    @Override
    public void sendChangePasswordEmail(EmailCaptchaRequestDTO request) {
        User user = SecurityUtils.getCurrentUser();
        String email = request.getEmail();
        String username = request.getUsername() != null ? request.getUsername() : "";
        if (user == null || !user.getEmail().equalsIgnoreCase(email) || !user.getUsername().equals(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱或用户名与当前账号不符");
        }
        emailService.sendChangePasswordEmail(request);
    }

    @Override
    public String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request) {
        User user = SecurityUtils.getCurrentUser();
        String email = request.getEmail();
        String username = request.getUsername() != null ? request.getUsername() : "";
        if (user == null || !user.getEmail().equalsIgnoreCase(email) || !user.getUsername().equals(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱或用户名与当前账号不符");
        }
        if (!emailService.verifyCode(email, request.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        String token = IdUtil.fastSimpleUUID();
        String redisKey = VREDIS_AUTH_CHANGE_PASSWORD_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, String.valueOf(user.getId()), Duration.ofMinutes(15));
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequestDTO request) {
        String redisKey = VREDIS_AUTH_CHANGE_PASSWORD_KEY_PREFIX + request.getToken();
        String userIdStr = redisTemplate.opsForValue().get(redisKey);
        if (userIdStr == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token已过期或无效");
        }
        Long tokenUserId = Long.parseLong(userIdStr);
        Long currentUserId = SecurityUtils.getCurrentUserIdSafe();
        if (currentUserId != null && !Objects.equals(currentUserId, tokenUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权使用此Token修改他人密码");
        }

        User user = userRepository.findById(tokenUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "原密码错误，请重新输入");
        }

        CompletableFuture<Boolean> samePasswordFuture = passwordEncoder.matchesAsync(request.getNewPassword(), user.getPassword());
        CompletableFuture<String> encodedNewPasswordFuture = passwordEncoder.encodeAsync(request.getNewPassword());

        if (Boolean.TRUE.equals(samePasswordFuture.join())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }

        user.setPassword(encodedNewPasswordFuture.join());
        userRepository.save(user);
        redisTemplate.delete(redisKey);
        tokenService.forceLogoutUser(user.getId());
        evictUserCache(user);

        SpringContextHolder.publishEvent(new SecurityNotificationEvent(
                this,
                user.getId(),
                SecurityEventType.PASSWORD_CHANGED,
                RequestUtil.getClientIp(),
                "修改账号密码成功"
        ));
    }

    @Override
    public void sendDeleteAccountEmail(EmailCaptchaRequestDTO request) {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        EmailCaptchaRequestDTO mailRequest = new EmailCaptchaRequestDTO();
        mailRequest.setEmail(user.getEmail());
        mailRequest.setUsername(user.getUsername());
        if (request != null) {
            mailRequest.setCaptcha(request.getCaptcha());
        }
        emailService.sendDeleteAccountEmail(mailRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(DeleteAccountRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!user.getUsername().equals(request.getUsername())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "确认用户名不匹配");
        }

        String email = user.getEmail().trim().toLowerCase();
        if (!emailService.verifyCode(email, request.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱验证码错误或已过期");
        }

        user.setStatus(false);
        user.setIsHidden(true);
        long timestamp = System.currentTimeMillis();
        String oldUsername = user.getUsername();
        String oldEmail = user.getEmail();

        String suffix = "_del_" + timestamp;
        String newUsername = (oldUsername.length() + suffix.length() > 64)
                ? oldUsername.substring(0, 64 - suffix.length()) + suffix
                : oldUsername + suffix;

        user.setUsername(newUsername);
        user.setEmail(oldEmail + suffix);
        if (user.getPhone() != null) {
            user.setPhone(null);
        }
        userProfileRepository.findById(user.getId()).ifPresent(p -> {
            p.setIsHidden(true);
            p.setBio(null);
            userProfileRepository.save(p);
        });

        userRepository.save(user);
        tokenService.forceLogoutUser(user.getId());

        try {
            tokenService.logout();
        } catch (Exception ignored) {
        }
        evictUserCache(oldUsername, oldEmail);

        SpringContextHolder.publishEvent(new SecurityNotificationEvent(
                this,
                user.getId(),
                SecurityEventType.ACCOUNT_DELETED,
                RequestUtil.getClientIp(),
                "用户通过邮箱 2FA 注销账号"
        ));
    }


}
