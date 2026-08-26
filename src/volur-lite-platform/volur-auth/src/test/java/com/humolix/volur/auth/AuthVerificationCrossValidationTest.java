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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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
        when(systemSettingService.getBoolean("REGISTER_ENABLED", true)).thenReturn(true);
        when(systemSettingService.getBoolean("ENABLE_CAPTCHA", false)).thenReturn(true);
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
        when(systemSettingService.getBoolean("REGISTER_ENABLED", true)).thenReturn(true);
        when(systemSettingService.getBoolean("ENABLE_CAPTCHA", false)).thenReturn(true);
        when(captchaService.verifyTicket(eq("valid_slide_ticket"), eq(CaptchaScene.REGISTER))).thenReturn(true);
        when(systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)).thenReturn(false);
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
    public void testRegisterFailsWhenEmailVerificationFails() {
        when(systemSettingService.getBoolean("REGISTER_ENABLED", true)).thenReturn(true);
        when(systemSettingService.getBoolean("ENABLE_CAPTCHA", false)).thenReturn(false);
        when(systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)).thenReturn(true);
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

}
