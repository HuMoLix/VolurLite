package com.humolix.volur.verification;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.mail.dispatcher.FailoverMailDispatcher;
import com.humolix.volur.verification.mail.executor.MailTaskExecutor;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.CaptchaService;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.verification.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EmailServiceVerificationTest {

    private SystemSettingService systemSettingService;
    private CaptchaService captchaService;
    private UserRepository userRepository;
    private FailoverMailDispatcher mailDispatcher;
    private MailTaskExecutor mailTaskExecutor;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    private EmailService emailService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        captchaService = Mockito.mock(CaptchaService.class);
        userRepository = Mockito.mock(UserRepository.class);
        mailDispatcher = Mockito.mock(FailoverMailDispatcher.class);
        mailTaskExecutor = Mockito.mock(MailTaskExecutor.class);
        rateLimiter = Mockito.mock(RedisSlidingWindowRateLimiter.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        emailService = new EmailServiceImpl(
                systemSettingService,
                captchaService,
                userRepository,
                mailDispatcher,
                mailTaskExecutor,
                rateLimiter,
                redisTemplate
        );
    }

    @Test
    public void testSendVerificationEmailFailsWhenCaptchaIsInvalidBeforeCheckingUserExistence() {
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(true);
        when(captchaService.verifyCaptcha(eq("c_123"), eq("wrong_code"), eq(CaptchaScene.REGISTER))).thenReturn(false);

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptchaId("c_123");
        request.setCaptchaCode("wrong_code");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> emailService.sendVerificationEmail(request));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertEquals("图形验证码错误或已过期", ex.getMessage());

        // Ensure user existence check was NOT invoked
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    public void testSendVerificationEmailFailsWhenUserAlreadyExistsAfterCaptchaValid() {
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(true);
        when(captchaService.verifyCaptcha(eq("c_123"), eq("valid_code"), eq(CaptchaScene.REGISTER))).thenReturn(true);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptchaId("c_123");
        request.setCaptchaCode("valid_code");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> emailService.sendVerificationEmail(request));
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        Assertions.assertEquals("该邮箱已被注册", ex.getMessage());

        // Verify captcha was checked
        verify(captchaService).verifyCaptcha(eq("c_123"), eq("valid_code"), eq(CaptchaScene.REGISTER));
        // Verify existence was checked
        verify(userRepository).existsByEmail("user@example.com");
    }

    @Test
    public void testSendVerificationEmailSuccessWhenCaptchaValidAndEmailNew() {
        when(systemSettingService.getBoolean("system.captcha.enabled", false)).thenReturn(true);
        when(captchaService.verifyCaptcha(eq("c_123"), eq("valid_code"), eq(CaptchaScene.REGISTER))).thenReturn(true);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(systemSettingService.getBoolean("system.auth.require_email_verification", false)).thenReturn(false);

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptchaId("c_123");
        request.setCaptchaCode("valid_code");

        Assertions.assertDoesNotThrow(() -> emailService.sendVerificationEmail(request));

        verify(captchaService).verifyCaptcha(eq("c_123"), eq("valid_code"), eq(CaptchaScene.REGISTER));
        verify(userRepository).existsByEmail("user@example.com");
        verify(mailTaskExecutor).executeAsyncMail(any());
    }

}
