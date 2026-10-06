package com.humolix.volur.auth;

import com.humolix.volur.auth.domain.dto.RegisterRequestDTO;
import com.humolix.volur.auth.exception.InvalidCaptchaException;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.auth.service.impl.AuthServiceImpl;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import com.humolix.volur.verification.service.EmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class AuthVerificationCrossValidationTest {

    private AuthService authService;
    private CaptchaSDK captchaSDK;
    private EmailService emailService;
    private UserRepository userRepository;
    private UserProfileRepository userProfileRepository;
    private SystemSettingService systemSettingService;
    private StringRedisTemplate redisTemplate;
    private VolurCacheTemplate cacheTemplate;
    private RedisOps redisOps;

    @BeforeEach
    public void setup() {
        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);
        Sm3PasswordEncoder passwordEncoder = new Sm3PasswordEncoder();
        TokenService tokenService = Mockito.mock(TokenService.class);
        captchaSDK = Mockito.mock(CaptchaSDK.class);
        emailService = Mockito.mock(EmailService.class);
        userRepository = Mockito.mock(UserRepository.class);
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        cacheTemplate = Mockito.mock(VolurCacheTemplate.class);
        redisOps = Mockito.mock(RedisOps.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);

        authService = new AuthServiceImpl(
                authenticationManager,
                jwtUtil,
                passwordEncoder,
                tokenService,
                captchaSDK,
                emailService,
                userRepository,
                userProfileRepository,
                systemSettingService,
                cacheTemplate,
                redisOps,
                redisTemplate
        );
    }

    @Test
    public void testRegisterRequiresValidCaptchaTicketWhenCaptchaEnabled() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(false);
        Mockito.doThrow(new InvalidCaptchaException("人机验证凭证已失效，请重新验证"))
                .when(captchaSDK).verify(any(), eq("testuser"), eq(CaptchaScene.REGISTER));

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail("testuser@volur.local");
        request.setCaptcha(new CaptchaVerifyRequestDTO(null, null, "invalid_ticket", null, CaptchaScene.REGISTER, "testuser"));

        Assertions.assertThrows(InvalidCaptchaException.class, () -> authService.register(request));
    }

    @Test
    public void testRegisterPassesWhenCaptchaTicketIsValid() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(false);
        Mockito.doNothing().when(captchaSDK).verify(any(), eq("testuser"), eq(CaptchaScene.REGISTER));
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
        request.setCaptcha(new CaptchaVerifyRequestDTO(null, null, "valid_slide_ticket", null, CaptchaScene.REGISTER, "testuser"));

        Assertions.assertDoesNotThrow(() -> authService.register(request));
    }

    @Test
    public void testRegisterFailsWhenEmailVerificationRequiredAndEmailMissing() {
        when(systemSettingService.getBoolean("system.auth.registration_enabled", true)).thenReturn(true);
        when(systemSettingService.getBoolean(eq(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION), anyBoolean())).thenReturn(true);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("testuser");
        request.setPassword("Password123#");
        request.setEmail(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> authService.register(request));
        Assertions.assertEquals("邮箱不能为空", ex.getMessage());
        Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
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

