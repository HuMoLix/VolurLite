package com.humolix.volur.verification;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.mail.executor.MailTaskExecutor;
import com.humolix.volur.verification.mail.service.MailTransportService;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.verification.service.EmailVerificationCodeService;
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
    private CaptchaSDK captchaSDK;
    private MailTransportService mailTransportService;
    private EmailVerificationCodeService emailVerificationCodeService;
    private UserRepository userRepository;
    private MailTaskExecutor mailTaskExecutor;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    private EmailService emailService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        captchaSDK = Mockito.mock(CaptchaSDK.class);
        mailTransportService = Mockito.mock(MailTransportService.class);
        emailVerificationCodeService = Mockito.mock(EmailVerificationCodeService.class);
        userRepository = Mockito.mock(UserRepository.class);
        mailTaskExecutor = Mockito.mock(MailTaskExecutor.class);
        rateLimiter = Mockito.mock(RedisSlidingWindowRateLimiter.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        emailService = new EmailServiceImpl(
                systemSettingService,
                captchaSDK,
                mailTransportService,
                emailVerificationCodeService,
                userRepository,
                mailTaskExecutor,
                rateLimiter,
                redisTemplate
        );
    }

    @Test
    public void testSendVerificationEmailFailsWhenCaptchaIsInvalidBeforeCheckingUserExistence() {
        doThrow(new BusinessException(HttpStatus.BAD_REQUEST, "图形验证码错误或已过期"))
                .when(captchaSDK).verify(any(), eq("user@example.com"), eq(CaptchaScene.REGISTER));

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptcha(new CaptchaVerifyRequestDTO("c_123", "wrong_code", null, null, CaptchaScene.REGISTER, "testuser"));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> emailService.sendVerificationEmail(request));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        Assertions.assertEquals("图形验证码错误或已过期", ex.getMessage());

        // Ensure user existence check was NOT invoked
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    public void testSendVerificationEmailFailsWhenUserAlreadyExistsAfterCaptchaValid() {
        doNothing().when(captchaSDK).verify(any(), eq("user@example.com"), eq(CaptchaScene.REGISTER));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptcha(new CaptchaVerifyRequestDTO("c_123", "valid_code", null, null, CaptchaScene.REGISTER, "testuser"));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> emailService.sendVerificationEmail(request));
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        Assertions.assertEquals("该邮箱已被注册", ex.getMessage());

        // Verify captcha was checked
        verify(captchaSDK).verify(any(), eq("user@example.com"), eq(CaptchaScene.REGISTER));
        // Verify existence was checked
        verify(userRepository).existsByEmail("user@example.com");
    }

    @Test
    public void testSendVerificationEmailSuccessWhenCaptchaValidAndEmailNew() {
        doNothing().when(captchaSDK).verify(any(), eq("user@example.com"), eq(CaptchaScene.REGISTER));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true)).thenReturn(false);
        when(systemSettingService.getSettingValue(eq("system.mail.from_address"), anyString())).thenReturn("noreply@volur.local");
        when(emailVerificationCodeService.generateAndStoreCode("user@example.com")).thenReturn("123456");

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setCaptcha(new CaptchaVerifyRequestDTO("c_123", "valid_code", null, null, CaptchaScene.REGISTER, "testuser"));

        Assertions.assertDoesNotThrow(() -> emailService.sendVerificationEmail(request));

        verify(captchaSDK).verify(any(), eq("user@example.com"), eq(CaptchaScene.REGISTER));
        verify(userRepository).existsByEmail("user@example.com");
        verify(mailTaskExecutor).executeAsyncMail(any());
    }

    @Test
    public void testSendVerificationEmailThrowsWhenFromAddressNotConfigured() {
        doNothing().when(captchaSDK).verify(any(), any(), any());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(systemSettingService.getSettingValue(eq("system.mail.from_address"), anyString())).thenReturn("");
        when(emailVerificationCodeService.generateAndStoreCode("user@example.com")).thenReturn("123456");

        EmailCaptchaRequestDTO request = new EmailCaptchaRequestDTO();
        request.setEmail("user@example.com");
        request.setUsername("testuser");

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> emailService.sendVerificationEmail(request));
        Assertions.assertTrue(ex.getMessage().contains("system.mail.from_address"));
    }

    @Test
    public void testSendEmail_unifiedPipeline_async() {
        when(systemSettingService.getSettingValue(eq("system.mail.from_address"), anyString())).thenReturn("noreply@volur.local");

        com.humolix.volur.verification.domain.bo.SendEmailBO bo = com.humolix.volur.verification.domain.bo.SendEmailBO.builder()
                .to("athlete@volur.local")
                .subject("开赛通知")
                .content("<p>比赛已开始</p>")
                .type("COMPETITION")
                .checkRateLimit(true)
                .async(true)
                .build();

        Assertions.assertDoesNotThrow(() -> emailService.sendEmail(bo));
        verify(mailTaskExecutor).executeAsyncMail(argThat(msg ->
                "athlete@volur.local".equals(msg.getTo())
                        && "开赛通知".equals(msg.getSubject())
                        && "COMPETITION".equals(msg.getType())
        ));
    }

    @Test
    public void testSendEmail_syncWithoutRateLimit() {
        when(systemSettingService.getSettingValue(eq("system.mail.from_address"), anyString())).thenReturn("noreply@volur.local");

        com.humolix.volur.verification.domain.bo.SendEmailBO bo = com.humolix.volur.verification.domain.bo.SendEmailBO.builder()
                .to("admin@volur.local")
                .subject("系统警报")
                .content("磁盘空间不足")
                .type("ALERT")
                .checkRateLimit(false)
                .async(false)
                .build();

        Assertions.assertDoesNotThrow(() -> emailService.sendEmail(bo));
        verify(mailTransportService).sendEmail(eq("admin@volur.local"), eq("系统警报"), eq("磁盘空间不足"), eq("ALERT"));
    }

    @Test
    public void testSendEmail_convenienceOverloads() {
        when(systemSettingService.getSettingValue(eq("system.mail.from_address"), anyString())).thenReturn("noreply@volur.local");

        Assertions.assertDoesNotThrow(() -> emailService.sendEmail("user@volur.local", "普通通知", "这是一封纯文本测试邮件"));
        verify(mailTaskExecutor).executeAsyncMail(argThat(msg ->
                "user@volur.local".equals(msg.getTo()) && "普通通知".equals(msg.getSubject())
        ));
    }

}

