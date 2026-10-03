package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.ChallengeOptionVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;

import java.util.List;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.RecaptchaService;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class SigninServiceImplTest {

    private TransitionLockManager transitionLockManager;
    private RecaptchaService recaptchaService;
    private FlowCryptoService flowCryptoService;
    private RiskEvaluationEngine riskEvaluationEngine;
    private CaptchaSDK captchaSDK;
    private UserRepository userRepository;
    private Sm3PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private SystemSettingService systemSettingService;
    private RedisOps redisOps;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthCookieService authCookieService;
    private SigninServiceImpl signinService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        recaptchaService = Mockito.mock(RecaptchaService.class);
        flowCryptoService = Mockito.mock(FlowCryptoService.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        captchaSDK = Mockito.mock(CaptchaSDK.class);
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(Sm3PasswordEncoder.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        redisOps = Mockito.mock(RedisOps.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        authCookieService = Mockito.mock(AuthCookieService.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        signinService = new SigninServiceImpl(
                transitionLockManager,
                recaptchaService,
                flowCryptoService,
                riskEvaluationEngine,
                captchaSDK,
                userRepository,
                passwordEncoder,
                jwtUtil,
                systemSettingService,
                redisOps,
                redisTemplate,
                authCookieService
        );
    }

    @Test
    void identifier_userExists_shouldAdvanceToPwd() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("testuser");
        dto.setDsh("S-100200300:1790000000");
        dto.setDeviceFingerprint("fp_sample_123");

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dto.getDsh())
                .currentStep("init")
                .cid(0)
                .build();

        given(transitionLockManager.createOrGetSession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn(null);

        User mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setEmail("testuser@example.com");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.advanceTransitionTicket(eq(dto.getDsh()), isNull(), eq(0), eq("identifier"), eq(1), eq("pwd"), eq("testuser")))
                .willReturn("ADG-NEXT_PWD_TICKET");

        SignInFlowResponseVO response = signinService.identifier(dto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("pwd", response.getNextStep());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-NEXT_PWD_TICKET", response.getTl());
    }

    @Test
    void identifier_userNotFound_shouldRecordFailureAndThrow() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("nonexistent");
        dto.setDsh("S-100200300:1790000000");
        dto.setDeviceFingerprint("fp_sample_123");

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dto.getDsh())
                .currentStep("init")
                .cid(0)
                .build();

        given(transitionLockManager.createOrGetSession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn("2");
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());
        given(userRepository.findByEmail("nonexistent")).willReturn(Optional.empty());

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> signinService.identifier(dto));
        Assertions.assertEquals("用户名或邮箱不存在", ex.getMessage());

        verify(redisOps).incr(startsWith("vredis:auth:identifier_failures:"), eq(1L), any());
    }

    @Test
    void identifier_failuresReachFive_shouldAdvanceToRecaptcha() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("anyuser");
        dto.setDsh("S-100200300:1790000000");
        dto.setDeviceFingerprint("fp_sample_123");

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dto.getDsh())
                .currentStep("init")
                .cid(0)
                .build();

        given(transitionLockManager.createOrGetSession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn("5");
        given(transitionLockManager.advanceTransitionTicket(eq(dto.getDsh()), isNull(), eq(0), eq("identifier"), eq(1), eq("recaptcha"), eq("anyuser")))
                .willReturn("ADG-RECAPTCHA_TICKET");

        SignInFlowResponseVO response = signinService.identifier(dto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("recaptcha", response.getNextStep());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-RECAPTCHA_TICKET", response.getTl());
    }

    @Test
    void challenge_recaptcha_success_shouldAdvanceToPwd() {
        SignInChallengeRequestDTO request = new SignInChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-RECAPTCHA_TICKET");
        request.setCid(1);
        request.setChallengeType("recaptcha");
        request.setCaptchaTicket("rctk_sample_valid");

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(request.getDsh())
                .identifier("validuser")
                .currentStep("recaptcha")
                .cid(1)
                .deviceFingerprint("fp_sample_123")
                .build();

        given(transitionLockManager.getSession(request.getDsh())).willReturn(session);
        given(recaptchaService.validateAndBurnTicket(eq(request.getDsh()), eq("rctk_sample_valid"))).willReturn(true);

        User mockUser = new User();
        mockUser.setUsername("validuser");
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.advanceTransitionTicket(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("recaptcha"), eq(2), eq("pwd"), eq("validuser")))
                .willReturn("ADG-PWD_TICKET");

        SignInFlowResponseVO response = signinService.challenge(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("pwd", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-PWD_TICKET", response.getTl());
    }

    @Test
    void challenge_recaptcha_userNotFound_shouldAdvanceToIdentifierAndSetError() {
        SignInChallengeRequestDTO request = new SignInChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-RECAPTCHA_TICKET");
        request.setCid(1);
        request.setChallengeType("recaptcha");
        request.setCaptchaTicket("rctk_sample_valid");

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(request.getDsh())
                .identifier("nonexistent")
                .currentStep("recaptcha")
                .cid(1)
                .deviceFingerprint("fp_sample_123")
                .build();

        given(transitionLockManager.getSession(request.getDsh())).willReturn(session);
        given(recaptchaService.validateAndBurnTicket(eq(request.getDsh()), eq("rctk_sample_valid"))).willReturn(true);
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());
        given(userRepository.findByEmail("nonexistent")).willReturn(Optional.empty());
        given(transitionLockManager.advanceTransitionTicket(eq(request.getDsh()), eq(request.getTl()), eq(1), eq("recaptcha"), eq(2), eq("identifier"), eq("nonexistent")))
                .willReturn("ADG-IDENTIFIER_TICKET");

        SignInFlowResponseVO response = signinService.challenge(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("identifier", response.getNextStep());
        Assertions.assertEquals(2, response.getCid());
        Assertions.assertEquals("ADG-IDENTIFIER_TICKET", response.getTl());
        Assertions.assertEquals("用户名或邮箱不存在", response.getErrorMessage());
        verify(redisOps).incr(startsWith("vredis:auth:identifier_failures:"), eq(1L), any());
    }

    @Test
    void getChallengeSelection_fromPwd_shouldReturnFiveMethods() {
        String dsh = "S-100200300:1790000000";
        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .identifier("testuser@example.com")
                .currentStep("pwd")
                .build();

        given(transitionLockManager.getSession(dsh)).willReturn(session);

        User mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setEmail("testuser@example.com");
        mockUser.setPhone("13800138000");
        given(userRepository.findByUsername("testuser@example.com")).willReturn(Optional.empty());
        given(userRepository.findByEmail("testuser@example.com")).willReturn(Optional.of(mockUser));

        List<ChallengeOptionVO> options = signinService.getChallengeSelection(dsh, "pwd");

        Assertions.assertNotNull(options);
        Assertions.assertEquals(5, options.size());
        Assertions.assertTrue(options.stream().noneMatch(opt -> "pwd".equalsIgnoreCase(opt.getType())));
        Assertions.assertTrue(options.stream().anyMatch(opt -> "pk".equalsIgnoreCase(opt.getType())));
        Assertions.assertTrue(options.stream().anyMatch(opt -> "etotp".equalsIgnoreCase(opt.getType())));
        Assertions.assertTrue(options.stream().anyMatch(opt -> "totp".equalsIgnoreCase(opt.getType())));
        Assertions.assertTrue(options.stream().anyMatch(opt -> "atcr".equalsIgnoreCase(opt.getType())));
        Assertions.assertTrue(options.stream().anyMatch(opt -> "backup_code".equalsIgnoreCase(opt.getType())));
    }

    @Test
    void challenge_pwd_success_shouldWriteAuthCookiesAndReturnSuccess() {
        SignInChallengeRequestDTO request = new SignInChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setCid(2);
        request.setChallengeType("pwd");
        request.setPassword("correctPassword123!");
        request.setRememberMe(true);

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(request.getDsh())
                .identifier("validuser")
                .currentStep("pwd")
                .cid(2)
                .deviceFingerprint("fp_sample_123")
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(com.humolix.volur.user.domain.enums.UserRole.USER);

        given(transitionLockManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("correctPassword123!", "encodedPassword")).willReturn(true);
        given(jwtUtil.generateAccessTokenFromUser(eq(mockUser), anyString())).willReturn("test_access_token");
        given(jwtUtil.generateRefreshTokenFromUser(eq(mockUser), anyString(), eq(true))).willReturn("test_refresh_token");

        SignInFlowResponseVO response = signinService.challenge(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("SUCCESS", response.getStatus());
        Assertions.assertEquals("validuser", response.getIdentifier());
        verify(transitionLockManager).burnAndCompleteSession(request.getDsh(), request.getTl(), 2, "pwd");
        verify(authCookieService).writeAuthCookies(eq("test_access_token"), eq("test_refresh_token"), eq(true));
    }

}
