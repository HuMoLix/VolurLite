package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.entity.UserProfile;
import com.humolix.volur.user.entity.Role;
import com.humolix.volur.user.event.UserStatusAction;
import com.humolix.volur.user.event.UserStatusChangedEvent;
import com.humolix.volur.auth.exception.UserOrEmailExistException;
import com.humolix.volur.auth.exception.TokenAlreadyBlacklistedException;
import com.humolix.volur.auth.exception.TokenInvalidException;
import com.humolix.volur.auth.exception.InvalidCaptchaException;
import com.humolix.volur.user.repository.RoleRepository;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.security.jwt.exception.MissingTokenException;
import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.service.CaptchaService;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SpringContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final Sm3PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final SystemSettingService systemSettingService;
    private final StringRedisTemplate redisTemplate;
    private final Optional<CacheManager> cacheManager;

    private static final String VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX = "vredis:auth:login_attempts:";
    private static final String VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX = "vredis:auth:login_lock:";
    private static final String VREDIS_AUTH_PASSWORD_RESET_KEY_PREFIX = "vredis:verification:password_reset:";
    private static final String VREDIS_AUTH_CHANGE_PASSWORD_KEY_PREFIX = "vredis:auth:change_password:";
    private static final String CACHE_USER_DETAILS = "user_details";

    private void evictUserCache(User user) {
        if (user != null) {
            evictUserCache(user.getUsername(), user.getEmail());
        }
    }

    private void evictUserCache(String username, String email) {
        if (cacheManager.isPresent()) {
            Cache cache = cacheManager.get().getCache(CACHE_USER_DETAILS);
            if (cache != null) {
                if (StringUtils.hasText(username)) {
                    cache.evict(username.trim());
                    cache.evict(username.trim().toLowerCase());
                }
                if (StringUtils.hasText(email)) {
                    cache.evict(email.trim());
                    cache.evict(email.trim().toLowerCase());
                }
            }
        }
    }

    private void checkCaptchaIfEnabled(String captchaId, String captchaCode, String captchaTicket, CaptchaScene scene) throws InvalidCaptchaException {
        if (systemSettingService.getBoolean("ENABLE_CAPTCHA", false)) {
            if (StringUtils.hasText(captchaTicket)) {
                if (!captchaService.verifyTicket(captchaTicket, scene)) {
                    throw new InvalidCaptchaException("人机验证凭证已失效，请重新验证");
                }
            } else if (StringUtils.hasText(captchaId) && StringUtils.hasText(captchaCode)) {
                if (!captchaService.verifyCaptcha(captchaId, captchaCode, scene)) {
                    throw new InvalidCaptchaException("图形验证码错误或已过期");
                }
            } else {
                throw new InvalidCaptchaException("请先完成人机安全验证");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequestDTO request) throws InvalidCaptchaException, UserOrEmailExistException {
        if (!systemSettingService.getBoolean("REGISTER_ENABLED", true)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "系统已关闭注册功能");
        }

        checkCaptchaIfEnabled(request.getCaptchaId(), request.getCaptchaCode(), request.getCaptchaTicket(), CaptchaScene.REGISTER);

        String normalizedUsername = request.getUsername().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername) || userRepository.existsByEmail(normalizedEmail)) {
            throw new UserOrEmailExistException();
        }

        String whitelistStr = systemSettingService.getSettingValue("EMAIL_WHITELIST_SUFFIXES", "");
        if (StringUtils.hasText(whitelistStr)) {
            String emailDomain = normalizedEmail.substring(normalizedEmail.lastIndexOf("@") + 1);
            boolean isWhitelisted = Arrays.stream(whitelistStr.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(StringUtils::hasText)
                    .map(suffix -> suffix.startsWith("@") ? suffix.substring(1) : suffix)
                    .anyMatch(allowedDomain -> emailDomain.equals(allowedDomain) || emailDomain.endsWith("." + allowedDomain));
            if (!isWhitelisted) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
            }
        }

        if (systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)) {
            if (!emailService.verifyCode(normalizedEmail, request.getEmailCaptcha())) {
                throw new InvalidCaptchaException("邮箱验证码错误或已过期");
            }
        }

        String defaultRoleName = systemSettingService.getSettingValue("DEFAULT_USER_ROLE", "USER");
        Role userRole = roleRepository.findByName(defaultRoleName)
                .orElseGet(() -> roleRepository.findByName("USER")
                        .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "系统默认角色配置缺失")));

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .role(userRole)
                .status(true)
                .totalPoints(0.0)
                .solves(0L)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserOrEmailExistException();
        }

        UserProfile userProfile = UserProfile.builder()
                .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
                .user(savedUser)
                .isHidden(false)
                .build();

        userProfileRepository.save(userProfile);

        SpringContextHolder.publishEvent(new UserStatusChangedEvent(this, savedUser.getId(), UserStatusAction.REGISTER));
    }

    @Override
    public AuthenticateResponseVO authenticate(AuthenticateRequestDTO request) throws InvalidCaptchaException {
        checkCaptchaIfEnabled(request.getCaptchaId(), request.getCaptchaCode(), request.getCaptchaTicket(), CaptchaScene.LOGIN);

        String rawAccount = request.getUsername().trim();
        String normalizedAccount = rawAccount.toLowerCase();

        int maxAttempts = systemSettingService.getInt("MAX_LOGIN_ATTEMPTS", 5);
        String lockKey = VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX + normalizedAccount;
        String attemptsKey = VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX + normalizedAccount;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
        }

        try {
            Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                    rawAccount, request.getPassword()
            );
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            String jti = IdUtil.fastSimpleUUID();

            boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
            String accessToken = jwtUtil.generateAccessToken(authentication, jti);
            String refreshToken = jwtUtil.generateRefreshToken(authentication, jti, rememberMe);

            redisTemplate.delete(attemptsKey);

            return AuthenticateResponseVO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (AuthenticationException ex) {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts >= maxAttempts) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(15));
                redisTemplate.delete(attemptsKey);
                throw new BusinessException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定15分钟");
            } else {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(30));
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
            }
        }
    }

    @Override
    public AuthenticateResponseVO refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "无效或已过期的刷新令牌，请重新登录");
        }

        Claims claims = jwtUtil.getClaimsFromToken(refreshToken);
        String oldJti = claims.getId();
        if (tokenService.isTokenBlacklisted(oldJti)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "会话已注销，请重新登录");
        }

        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被封禁");
        }

        long iat = claims.getIssuedAt().getTime();
        if (tokenService.isUserForceLoggedOut(user.getId(), iat)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "会话已失效，请重新登录");
        }

        Boolean rememberMeClaim = claims.get("rememberMe", Boolean.class);
        boolean rememberMe = Boolean.TRUE.equals(rememberMeClaim);

        tokenService.blacklistToken(oldJti, claims.getExpiration().getTime());
        String newJti = IdUtil.fastSimpleUUID();
        String newAccessToken = jwtUtil.generateAccessTokenFromUser(user, newJti);
        String newRefreshToken = jwtUtil.generateRefreshTokenFromUser(user, newJti, rememberMe);

        return AuthenticateResponseVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void processForgotPassword(ForgotPasswordRequestDTO request) {
        checkCaptchaIfEnabled(request.getCaptchaId(), request.getCaptchaCode(), request.getCaptchaTicket(), CaptchaScene.RESET_PASSWORD);
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!emailService.verifyCode(normalizedEmail, request.getCode().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "绑定的用户不存在"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        tokenService.forceLogoutUser(user.getId());
        evictUserCache(user);
    }

    @Override
    public void sendVerificationEmail(EmailCaptchaRequestDTO request) {
        emailService.sendVerificationEmail(request);
    }

    @Override
    public void sendChangePasswordEmail(EmailCaptchaRequestDTO request) {
        User user = SecurityUtils.getCurrentUser();
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        if (user == null || !user.getEmail().equalsIgnoreCase(normalizedEmail) || !user.getUsername().equals(normalizedUsername)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱或用户名与当前账号不符");
        }
        emailService.sendChangePasswordEmail(request);
    }

    @Override
    public String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request) {
        User user = SecurityUtils.getCurrentUser();
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        if (user == null || !user.getEmail().equalsIgnoreCase(normalizedEmail) || !user.getUsername().equals(normalizedUsername)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱或用户名与当前账号不符");
        }
        if (!emailService.verifyCode(normalizedEmail, request.getCode().trim())) {
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
        String redisKey = VREDIS_AUTH_CHANGE_PASSWORD_KEY_PREFIX + request.getToken().trim();
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

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(redisKey);
        tokenService.forceLogoutUser(user.getId());
        evictUserCache(user);
    }

    @Override
    public void logout(HttpServletRequest request) throws TokenAlreadyBlacklistedException, TokenInvalidException {
        String token = jwtUtil.parseToken(request);
        if (!StringUtils.hasText(token)) {
            throw new MissingTokenException("未找到有效的令牌");
        }

        Claims claims;
        try {
            claims = jwtUtil.getClaimsFromToken(token);
        } catch (Exception ex) {
            throw new TokenInvalidException("令牌错误或已过期");
        }

        String jti = claims.getId();
        if (tokenService.isTokenBlacklisted(jti)) {
            throw new TokenAlreadyBlacklistedException();
        }

        try {
            long expiration = claims.getExpiration().getTime();
            tokenService.blacklistToken(jti, expiration);
            SecurityUtils.logout();
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "登出失败: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

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
        if (user.getUserProfile() != null) {
            user.getUserProfile().setIsHidden(true);
            user.getUserProfile().setBio(null);
        }

        userRepository.save(user);
        tokenService.forceLogoutUser(user.getId());

        try {
            logout(request);
        } catch (Exception ignored) {
        }
        evictUserCache(oldUsername, oldEmail);
    }

}
