package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.dto.ChallengeSelectRequestDTO;
import com.humolix.volur.security.domain.dto.EmailOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.ReCAPTCHAChallengeSubmitDTO;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.vo.ChallengeOptionVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.PasskeyService;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.service.EmailService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class ChallengeServiceImplTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;
    private SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private UserTrustedDeviceService userTrustedDeviceService;
    private UserRepository userRepository;
    private EmailService emailService;
    private RecaptchaService recaptchaService;
    private FlowCryptoService flowCryptoService;
    private PasskeyService passkeyService;
    private RiskEvaluationEngine riskEvaluationEngine;
    private SystemSettingService systemSettingService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ChallengeServiceImpl challengeService;

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
        recaptchaService = Mockito.mock(RecaptchaService.class);
        flowCryptoService = Mockito.mock(FlowCryptoService.class);
        passkeyService = Mockito.mock(PasskeyService.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        challengeService = new ChallengeServiceImpl(
                sessionManager,
                transitionLockManager,
                sessionDeviceBaselineValidator,
                userTrustedDeviceService,
                userRepository,
                emailService,
                recaptchaService,
                flowCryptoService,
                passkeyService,
                riskEvaluationEngine,
                systemSettingService,
                redisTemplate
        );
    }

    @Test
    void verifyReCAPTCHA_success_shouldAdvanceToPwd() {
        ReCAPTCHAChallengeSubmitDTO request = new ReCAPTCHAChallengeSubmitDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-RECAPTCHA_TICKET");
        request.setCid(1);
        request.setRecaptchaTicket("rctk_sample_valid");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("recaptcha").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(recaptchaService.validateAndBurnTicket(eq(request.getDsh()), eq("rctk_sample_valid"))).willReturn(true);

        User mockUser = new User();
        mockUser.setUsername("validuser");
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("recaptcha"), eq(2), eq("pwd"), eq("validuser")))
                .willReturn("ADG-PWD_TICKET");

        FlowResponseVO response = challengeService.verifyReCAPTCHA(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("pwd", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-PWD_TICKET", response.getTl());
    }


    @Test
    void verifyEmailOTP_success_shouldAdvanceToAuthenticate() {
        EmailOTPChallengeRequestDTO request = new EmailOTPChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-ETOTP_TICKET");
        request.setCid(2);
        request.setCode("123456");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("etotp").cid(2).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(emailService.verifyCode(eq("valid@example.com"), eq("123456"))).willReturn(true);
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(2), eq("etotp"), eq(3), eq("authenticate"), eq("validuser")))
                .willReturn("ADG-AUTH_TICKET");

        FlowResponseVO response = challengeService.verifyEmailOTP(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("authenticate", response.getNextStep());
        Assertions.assertEquals(3, response.getCid());
        Assertions.assertEquals("ADG-AUTH_TICKET", response.getTl());
    }

    @Test
    void selectChallenge_toEtotp_shouldRequireRecaptchaAndNotSendEmailYet() {
        ChallengeSelectRequestDTO request = new ChallengeSelectRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(1);
        request.setTargetChallengeType("etotp");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("pwd").cid(1).build())
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setEmail("valid@example.com");

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("pwd"), eq(1), eq("recaptcha"), eq("validuser")))
                .willReturn("ADG-RECAPTCHA_TICKET");

        FlowResponseVO response = challengeService.selectChallenge(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("CHALLENGE", response.getStatus());
        Assertions.assertEquals("recaptcha", response.getNextStep());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-RECAPTCHA_TICKET", response.getTl());
        Mockito.verify(sessionManager).setMetadata(eq(request.getDsh()), eq("pendingChallenge"), eq("etotp"));
        Mockito.verify(emailService, Mockito.never()).sendVerificationEmail(anyString(), anyString(), Mockito.anyBoolean());
    }

    @Test
    void verifyReCAPTCHA_withPendingEtotp_shouldSendEmailAndAdvanceToEtotp() {
        ReCAPTCHAChallengeSubmitDTO request = new ReCAPTCHAChallengeSubmitDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-RECAPTCHA_TICKET");
        request.setCid(1);
        request.setRecaptchaTicket("rctk_sample_valid");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("recaptcha").cid(1).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(recaptchaService.validateAndBurnTicket(eq(request.getDsh()), eq("rctk_sample_valid"))).willReturn(true);
        given(sessionManager.getMetadata(eq(request.getDsh()), eq("pendingChallenge"))).willReturn("etotp");

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setEmail("valid@example.com");
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("recaptcha"), eq(2), eq("etotp"), eq("validuser")))
                .willReturn("ADG-ETOTP_TICKET");

        FlowResponseVO response = challengeService.verifyReCAPTCHA(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("etotp", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-ETOTP_TICKET", response.getTl());
        verify(emailService).sendVerificationEmail(eq("valid@example.com"), eq("validuser"), eq(true));
        verify(sessionManager).removeMetadata(eq(request.getDsh()), eq("pendingChallenge"));
    }

    @Test
    void verifyReCAPTCHA_withWaitingStepsFallback_shouldSendEmailAndAdvanceToEtotp() {
        ReCAPTCHAChallengeSubmitDTO request = new ReCAPTCHAChallengeSubmitDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-RECAPTCHA_TICKET");
        request.setCid(1);
        request.setRecaptchaTicket("rctk_sample_valid");

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder()
                        .identifier("validuser")
                        .currentStep("recaptcha")
                        .waitingSteps(List.of("recaptcha", "etotp", "authenticate"))
                        .cid(1)
                        .build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(recaptchaService.validateAndBurnTicket(eq(request.getDsh()), eq("rctk_sample_valid"))).willReturn(true);
        given(sessionManager.getMetadata(eq(request.getDsh()), eq("pendingChallenge"))).willReturn(null);

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setEmail("valid@example.com");
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("recaptcha"), eq(2), eq("etotp"), eq("validuser")))
                .willReturn("ADG-ETOTP_TICKET_FALLBACK");

        FlowResponseVO response = challengeService.verifyReCAPTCHA(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("etotp", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-ETOTP_TICKET_FALLBACK", response.getTl());
        verify(emailService).sendVerificationEmail(eq("valid@example.com"), eq("validuser"), eq(true));
    }

    @Test
    void getChallengeSelection_shouldFilterCurrentTypeAndReturnOptions() {
        String dsh = "S-100200300:1790000000";
        Session session = Session.builder()
                .dsh(dsh)
                .flow(Flow.builder().identifier("validuser").build())
                .build();

        User mockUser = new User();
        mockUser.setId(1001L);
        mockUser.setUsername("validuser");
        mockUser.setEmail("developer@volur.com");
        mockUser.setPhone("13800138000");

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passkeyService.hasPasskey(1001L)).willReturn(true);

        List<ChallengeOptionVO> options = challengeService.getChallengeSelection(dsh, "pwd");

        Assertions.assertNotNull(options);
        Assertions.assertFalse(options.isEmpty());
        boolean hasPwd = options.stream().anyMatch(o -> "pwd".equalsIgnoreCase(o.getType()));
        Assertions.assertFalse(hasPwd);
        boolean hasEtotp = options.stream().anyMatch(o -> "etotp".equalsIgnoreCase(o.getType()));
        Assertions.assertTrue(hasEtotp);
        boolean hasSmsotp = options.stream().anyMatch(o -> "smsotp".equalsIgnoreCase(o.getType()));
        Assertions.assertTrue(hasSmsotp);
        boolean hasPk = options.stream().anyMatch(o -> "pk".equalsIgnoreCase(o.getType()));
        Assertions.assertTrue(hasPk);
    }

    @Test
    void getChallengeSelection_whenNoOtherMethods_returnsEmptyList() {
        String dsh = "S-100200300:1790000000";
        Session session = Session.builder()
                .dsh(dsh)
                .flow(Flow.builder().identifier("adminuser").build())
                .build();

        User mockUser = new User();
        mockUser.setId(1002L);
        mockUser.setUsername("adminuser");
        mockUser.setEmail(null);
        mockUser.setPhone(null);

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(userRepository.findByUsername("adminuser")).willReturn(Optional.of(mockUser));
        given(passkeyService.hasPasskey(1002L)).willReturn(false);

        List<ChallengeOptionVO> options = challengeService.getChallengeSelection(dsh, "pwd");

        Assertions.assertNotNull(options);
        Assertions.assertTrue(options.isEmpty());
    }

}
