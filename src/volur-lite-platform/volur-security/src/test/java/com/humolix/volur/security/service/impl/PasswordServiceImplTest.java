package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.service.EmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PasswordServiceImplTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;
    private SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private UserTrustedDeviceService userTrustedDeviceService;
    private UserRepository userRepository;
    private EmailService emailService;
    private FlowCryptoService flowCryptoService;
    private Sm3PasswordEncoder passwordEncoder;
    private RiskEvaluationEngine riskEvaluationEngine;
    private SystemSettingService systemSettingService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private PasswordServiceImpl passwordService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sessionManager = Mockito.mock(SessionManager.class);
        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        sessionDeviceBaselineValidator = Mockito.mock(SessionDeviceBaselineValidator.class);
        userTrustedDeviceService = Mockito.mock(UserTrustedDeviceService.class);
        userRepository = Mockito.mock(UserRepository.class);
        emailService = Mockito.mock(EmailService.class);
        flowCryptoService = Mockito.mock(FlowCryptoService.class);
        passwordEncoder = Mockito.mock(Sm3PasswordEncoder.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        passwordService = new PasswordServiceImpl(
                sessionManager,
                transitionLockManager,
                sessionDeviceBaselineValidator,
                userTrustedDeviceService,
                userRepository,
                emailService,
                flowCryptoService,
                passwordEncoder,
                riskEvaluationEngine,
                systemSettingService,
                redisTemplate
        );
    }

    @Test
    void verifyPassword_success_trustedDevice_shouldAdvanceToAuthenticate() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(UserRole.USER);
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);
        given(userTrustedDeviceService.isTrustedDevice(eq(1001L), anyString())).willReturn(true);
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("pwd"), eq(2), eq("authenticate"), eq("validuser")))
                .willReturn("ADG-AUTH_TICKET");

        FlowResponseVO response = passwordService.verifyPassword(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("authenticate", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-AUTH_TICKET", response.getTl());
    }

    @Test
    void verifyPassword_untrustedDevice_shouldTriggerEtotp() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(UserRole.USER);
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);
        given(userTrustedDeviceService.isTrustedDevice(eq(1001L), anyString())).willReturn(false);
        given(systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true)).willReturn(true);
        given(systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false)).willReturn(true);
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("pwd"), eq(2), eq("etotp"), eq("validuser")))
                .willReturn("ADG-ETOTP_TICKET");

        FlowResponseVO response = passwordService.verifyPassword(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("etotp", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-ETOTP_TICKET", response.getTl());
        verify(emailService).sendVerificationEmail(eq("valid@example.com"), eq("validuser"), eq(true));
    }

    @Test
    void verifyPassword_untrustedDevice_mailDisabled_shouldAdvanceToAuthenticate() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(UserRole.USER);
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);
        given(userTrustedDeviceService.isTrustedDevice(eq(1001L), anyString())).willReturn(false);
        given(systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true)).willReturn(true);
        given(systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false)).willReturn(false);
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("pwd"), eq(2), eq("authenticate"), eq("validuser")))
                .willReturn("ADG-AUTH_TICKET");

        FlowResponseVO response = passwordService.verifyPassword(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("authenticate", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
    }

    @Test
    void verifyPassword_untrustedDevice_requireEmailDisabled_shouldAdvanceToAuthenticate() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(UserRole.USER);
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);
        given(userTrustedDeviceService.isTrustedDevice(eq(1001L), anyString())).willReturn(false);
        given(systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true)).willReturn(false);
        given(systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false)).willReturn(true);
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("pwd"), eq(2), eq("authenticate"), eq("validuser")))
                .willReturn("ADG-AUTH_TICKET");

        FlowResponseVO response = passwordService.verifyPassword(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("authenticate", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
    }

    @Test
    void verifyPassword_missingIdentifier_shouldThrowBadRequest() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setPassword("any_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("").currentStep("pwd").cid(1).build())
                .build();

        given(sessionManager.getSession(request.getDsh())).willReturn(session);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> passwordService.verifyPassword(request));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void verifyPassword_whenPasswordWrong_shouldRecordFailureAndThrowUnauthorized() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("wrong_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(systemSettingService.getInt("system.auth.max_login_attempts", 5)).willReturn(5);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("wrong_password", "encodedPassword")).willReturn(false);
        given(valueOperations.increment("vredis:auth:login:attempts:validuser")).willReturn(1L);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> passwordService.verifyPassword(request));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(riskEvaluationEngine).recordFailure("127.0.0.1", CaptchaScene.LOGIN);
        verify(riskEvaluationEngine).recordAccountFailure("validuser", CaptchaScene.LOGIN);
    }

    @Test
    void verifyPassword_whenUserDisabled_shouldThrowForbidden() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(false);

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(systemSettingService.getInt("system.auth.max_login_attempts", 5)).willReturn(5);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> passwordService.verifyPassword(request));
        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void verifyPassword_whenMaintenanceModeAndNonAdmin_shouldThrowMaintenanceModeException() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setPassword("correct_password");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(UserRole.USER);

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(systemSettingService.getInt("system.auth.max_login_attempts", 5)).willReturn(5);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correct_password", "encodedPassword")).willReturn(true);
        given(systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false)).willReturn(true);
        given(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE), anyString()))
                .willReturn("系统正在维护");

        Assertions.assertThrows(MaintenanceModeException.class, () -> passwordService.verifyPassword(request));
    }

}
