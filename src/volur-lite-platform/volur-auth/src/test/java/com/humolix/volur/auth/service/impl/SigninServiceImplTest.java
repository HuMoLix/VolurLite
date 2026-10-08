package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.domain.dto.signin.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserRemoveRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserSelectRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInAuthenticateRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.risk.DeviceEnvironmentMatrixEvaluator;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.entity.UserTrustedDevice;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.repository.UserTrustedDeviceRepository;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.RiskLevel;
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
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class SigninServiceImplTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;
    private RiskEvaluationEngine riskEvaluationEngine;
    private UserRepository userRepository;
    private UserProfileRepository userProfileRepository;
    private UserTrustedDeviceRepository userTrustedDeviceRepository;
    private JwtUtil jwtUtil;
    private RedisOps redisOps;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthCookieService authCookieService;
    private DeviceEnvironmentMatrixEvaluator deviceEnvironmentMatrixEvaluator;
    private SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private SigninServiceImpl signinService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        request.setCookies(new jakarta.servlet.http.Cookie("_v_dtk", "fp_sample_123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sessionManager = Mockito.mock(SessionManager.class);
        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        userRepository = Mockito.mock(UserRepository.class);
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        userTrustedDeviceRepository = Mockito.mock(UserTrustedDeviceRepository.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        redisOps = Mockito.mock(RedisOps.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        authCookieService = Mockito.mock(AuthCookieService.class);
        deviceEnvironmentMatrixEvaluator = Mockito.mock(DeviceEnvironmentMatrixEvaluator.class);
        sessionDeviceBaselineValidator = Mockito.mock(SessionDeviceBaselineValidator.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(deviceEnvironmentMatrixEvaluator.evaluate(any(), any())).willReturn(RiskLevel.LOW);

        signinService = new SigninServiceImpl(
                sessionManager,
                transitionLockManager,
                riskEvaluationEngine,
                userRepository,
                userProfileRepository,
                userTrustedDeviceRepository,
                jwtUtil,
                redisOps,
                redisTemplate,
                authCookieService,
                deviceEnvironmentMatrixEvaluator,
                sessionDeviceBaselineValidator
        );
    }

    @Test
    void probeAccounts_withExistingAndNonExistingUsers_shouldReturnStatusAndDetails() {
        AccountChooserProbeRequestDTO request = new AccountChooserProbeRequestDTO(List.of("alice", "unknown"));

        User alice = new User();
        alice.setId(101L);
        alice.setUsername("alice");
        alice.setEmail("alice@volur.com");
        alice.setStatus(true);

        UserProfile profile = UserProfile.builder().id(101L).avatarUrl("https://img.volur.com/alice.png").build();

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(alice));
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());
        given(userRepository.findByEmail("unknown")).willReturn(Optional.empty());
        given(userProfileRepository.findById(101L)).willReturn(Optional.of(profile));

        AccountChooserProbeResponseVO response = signinService.probeAccounts(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.getAccounts().size());
        Assertions.assertEquals("alice", response.getAccounts().get(0).getUsername());
        Assertions.assertEquals("https://img.volur.com/alice.png", response.getAccounts().get(0).getAvatarUrl());
        Assertions.assertEquals("SIGNED_OUT", response.getAccounts().get(0).getSessionStatus());
        Assertions.assertEquals("unknown", response.getAccounts().get(1).getUsername());
        Assertions.assertEquals("SIGNED_OUT", response.getAccounts().get(1).getSessionStatus());
    }

    @Test
    void selectAccount_validRequest_shouldAdvanceToPwd() {
        AccountChooserSelectRequestDTO request = AccountChooserSelectRequestDTO.builder()
                .dsh("S-100200300:1790000000")
                .identifier("alice")
                .build();

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().currentStep("init").cid(0).build())
                .build();

        given(sessionManager.recoverySession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn(null);

        User mockUser = new User();
        mockUser.setUsername("alice");
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(any(), any(), eq(0), eq("identifier"), eq(1), eq("pwd"), eq("alice")))
                .willReturn("ADG-NEXT_TICKET");

        SignInFlowResponseVO response = signinService.selectAccount(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("pwd", response.getNextStep());
        Assertions.assertEquals("CHALLENGE", response.getStatus());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-NEXT_TICKET", response.getTl());
    }

    @Test
    void removeAccount_trustedDeviceExists_shouldDeleteDevice() {
        AccountChooserRemoveRequestDTO request = new AccountChooserRemoveRequestDTO("bob");
        User bob = new User();
        bob.setId(202L);
        bob.setUsername("bob");

        UserTrustedDevice device = UserTrustedDevice.builder().id(999L).userId(202L).deviceHash("fp_sample_123").build();

        given(userRepository.findByUsername("bob")).willReturn(Optional.of(bob));
        given(userTrustedDeviceRepository.findByUserIdAndDeviceHash(eq(202L), eq("fp_sample_123"))).willReturn(Optional.of(device));

        signinService.removeAccount(request);

        verify(userTrustedDeviceRepository).delete(device);
    }

    @Test
    void identifier_userExists_shouldAdvanceToPwd() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("testuser");
        dto.setDsh("S-100200300:1790000000");

        Session session = Session.builder()
                .dsh(dto.getDsh())
                .flow(Flow.builder().currentStep("init").cid(0).build())
                .build();

        given(sessionManager.recoverySession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn(null);

        User mockUser = new User();
        mockUser.setUsername("testuser");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(any(), any(), eq(0), eq("identifier"), eq(1), eq("pwd"), eq("testuser")))
                .willReturn("ADG-NEXT_TICKET");

        SignInFlowResponseVO response = signinService.identifier(dto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("pwd", response.getNextStep());
        Assertions.assertEquals("CHALLENGE", response.getStatus());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-NEXT_TICKET", response.getTl());
    }

    @Test
    void identifier_userNotFound_shouldRecordFailureAndThrowException() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("nonexistent");
        dto.setDsh("S-100200300:1790000000");

        Session session = Session.builder()
                .dsh(dto.getDsh())
                .flow(Flow.builder().currentStep("init").cid(0).build())
                .build();

        given(sessionManager.recoverySession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(valueOperations.get(anyString())).willReturn(null);
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());
        given(userRepository.findByEmail("nonexistent")).willReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> signinService.identifier(dto));

        verify(redisOps).incr(startsWith("vredis:auth:identifier_failures:"), eq(1L), any());
    }

    @Test
    void identifier_highRiskEnvironment_shouldAdvanceToRecaptchaFirst() {
        SignInIdentifierRequestDTO dto = new SignInIdentifierRequestDTO();
        dto.setIdentifier("testuser");
        dto.setDsh("S-100200300:1790000000");

        Session session = Session.builder()
                .dsh(dto.getDsh())
                .flow(Flow.builder().currentStep("init").cid(0).build())
                .build();

        given(sessionManager.recoverySession(any(), any(), any(), any(), any())).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.HIGH);
        given(valueOperations.get(anyString())).willReturn(null);
        given(transitionLockManager.nextTransLock(any(), any(), eq(0), eq("identifier"), eq(1), eq("recaptcha"), eq("testuser")))
                .willReturn("ADG-RECAPTCHA_TICKET");

        SignInFlowResponseVO response = signinService.identifier(dto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("recaptcha", response.getNextStep());
        Assertions.assertEquals("CHALLENGE", response.getStatus());
        Assertions.assertEquals(1, response.getCid());
        Assertions.assertEquals("ADG-RECAPTCHA_TICKET", response.getTl());
        Assertions.assertTrue(session.getFlow() instanceof com.humolix.volur.security.flow.flows.SigninFlow);
        Assertions.assertEquals(List.of("recaptcha", "pwd", "authenticate"), session.getFlow().getWaitingSteps());
    }

    @Test
    void authenticate_validRequest_shouldCompleteSessionAndIssueTokens() {
        SignInAuthenticateRequestDTO request = new SignInAuthenticateRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-AUTH_TICKET");
        request.setCid(3);
        request.setRememberMe(true);

        Session session = Session.builder()
                .dsh(request.getDsh())
                .flow(Flow.builder().identifier("validuser").currentStep("authenticate").cid(3).build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_sample_123"))
                .build();

        User mockUser = new User();
        mockUser.setUsername("validuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(true);
        mockUser.setRole(com.humolix.volur.user.domain.enums.UserRole.USER);

        given(sessionManager.getSession(request.getDsh())).willReturn(session);
        given(userRepository.findByUsername("validuser")).willReturn(Optional.of(mockUser));
        given(jwtUtil.generateAccessTokenFromUser(eq(mockUser), anyString())).willReturn("test_access_token");
        given(jwtUtil.generateRefreshTokenFromUser(eq(mockUser), anyString(), eq(true))).willReturn("test_refresh_token");

        SignInFlowResponseVO response = signinService.authenticate(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("SUCCESS", response.getStatus());
        Assertions.assertEquals("validuser", response.getIdentifier());
        verify(transitionLockManager).burnAndCompleteSession(request.getDsh(), request.getTl(), 3, "authenticate");
        verify(authCookieService).writeAuthCookies(eq("test_access_token"), eq("test_refresh_token"), eq(true));
    }

}
