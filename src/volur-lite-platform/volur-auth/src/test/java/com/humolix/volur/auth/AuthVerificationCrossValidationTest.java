package com.humolix.volur.auth;

import com.humolix.volur.auth.domain.dto.RegisterRequestDTO;
import com.humolix.volur.auth.exception.InvalidCaptchaException;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.auth.service.impl.AuthServiceImpl;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.service.CaptchaService;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AuthVerificationCrossValidationTest {

    private AuthService authService;
    private CaptchaService captchaService;
    private EmailService emailService;
    private UserRepository userRepository;
    private UserProfileRepository userProfileRepository;
    private SystemSettingService systemSettingService;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    public void setup() {
        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);
        Sm3PasswordEncoder passwordEncoder = new Sm3PasswordEncoder();
        TokenService tokenService = Mockito.mock(TokenService.class);
        captchaService = Mockito.mock(CaptchaService.class);
        emailService = Mockito.mock(EmailService.class);
        userRepository = Mockito.mock(UserRepository.class);
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);

        authService = new AuthServiceImpl(
                authenticationManager,
                jwtUtil,
                passwordEncoder,
                tokenService,
                captchaService,
                emailService,
                userRepository,
                userProfileRepository,
                systemSettingService,
                redisTemplate,
                Optional.empty()
        );
    }

    @Test
    public void testRegisterRequiresValidCaptchaTicketWhenCaptchaEnabled() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(false);
        when(captchaService.verifyTicket(eq("invalid_ticket"), eq(CaptchaScene.REGISTER))).thenReturn(false);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setCaptchaTicket("invalid_ticket");

        Assertions.assertThrows(InvalidCaptchaException.class, () -> authService.register(request));
    }

    @Test
    public void testRegisterPassesWhenCaptchaTicketIsValid() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(true);
        when(captchaService.verifyTicket(eq("valid_slide_ticket"), eq(CaptchaScene.REGISTER))).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@volur.local")).thenReturn(false);
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            com.humolix.volur.user.domain.entity.User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setCaptchaTicket("valid_slide_ticket");

        Assertions.assertDoesNotThrow(() -> authService.register(request));
    }

    @Test
    public void testRegisterFailsWhenEmailVerificationRequiredAndCodeMissing() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(true);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@volur.local")).thenReturn(false);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setEmailCaptcha("");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> authService.register(request));
        Assertions.assertEquals("邮箱验证码不能为空", ex.getMessage());
        Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void testRegisterFailsWhenEmailVerificationFails() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(false);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(true);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@volur.local")).thenReturn(false);
        when(emailService.verifyCode("testuser@volur.local", "wrong_code")).thenReturn(false);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setEmailCaptcha("wrong_code");

        Assertions.assertThrows(InvalidCaptchaException.class, () -> authService.register(request));
    }

    @Test
    public void testRegisterPassesWhenEmailVerificationSucceeds() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(true);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@volur.local")).thenReturn(false);
        when(emailService.verifyCode("testuser@volur.local", "123456")).thenReturn(true);
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            com.humolix.volur.user.domain.entity.User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setEmailCaptcha("123456");

        Assertions.assertDoesNotThrow(() -> authService.register(request));
    }

    @Test
    public void testRegisterFailsWhenUsernameExists() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");

        com.humolix.volur.auth.exception.UserOrEmailExistException ex =
                Assertions.assertThrows(com.humolix.volur.auth.exception.UserOrEmailExistException.class, () -> authService.register(request));
        Assertions.assertEquals("用户名或邮箱已存在", ex.getMessage());
        Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertInstanceOf(com.humolix.volur.framework.common.exception.BusinessException.class, ex);
    }

    @Test
    public void testRegisterFailsWhenEmailExists() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@volur.local")).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");

        com.humolix.volur.auth.exception.UserOrEmailExistException ex =
                Assertions.assertThrows(com.humolix.volur.auth.exception.UserOrEmailExistException.class, () -> authService.register(request));
        Assertions.assertEquals("用户名或邮箱已存在", ex.getMessage());
        Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
    }

}

