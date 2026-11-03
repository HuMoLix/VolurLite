package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.dto.TOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.TotpEnableRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.TotpEnableResponseVO;
import com.humolix.volur.security.domain.vo.TotpSetupVO;
import com.humolix.volur.security.domain.vo.TotpStatusVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.security.util.TotpUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class TotpServiceImplTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SystemSettingService systemSettingService;
    private UserRepository userRepository;
    private SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private UserTrustedDeviceService userTrustedDeviceService;
    private RiskEvaluationEngine riskEvaluationEngine;

    private TotpServiceImpl totpService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessionManager = Mockito.mock(SessionManager.class);
        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        systemSettingService = Mockito.mock(SystemSettingService.class);
        userRepository = Mockito.mock(UserRepository.class);
        sessionDeviceBaselineValidator = Mockito.mock(SessionDeviceBaselineValidator.class);
        userTrustedDeviceService = Mockito.mock(UserTrustedDeviceService.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        totpService = new TotpServiceImpl(
                sessionManager,
                transitionLockManager,
                redisTemplate,
                systemSettingService,
                userRepository,
                sessionDeviceBaselineValidator,
                userTrustedDeviceService,
                riskEvaluationEngine
        );
    }

    @Test
    void setupTotp_shouldGenerateSecretAndCache() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        Session session = Session.builder().dsh(dsh).build();

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(sessionManager.getMetadata(dsh, "username")).willReturn("alice");
        given(systemSettingService.getSettingValue(eq("system.security.webauthn.rp_name"), anyString())).willReturn("Volur Lite");

        TotpSetupVO res = totpService.setupTotp(dsh, tl);

        Assertions.assertNotNull(res);
        Assertions.assertEquals("alice", res.getAccount());
        Assertions.assertEquals("Volur Lite", res.getIssuer());
        Assertions.assertNotNull(res.getSecret());
        Assertions.assertTrue(res.getOtpAuthUrl().contains("alice"));

        verify(transitionLockManager).validateTransLock(dsh, tl, "pt/options");
        verify(valueOperations).set(eq("vredis:security:totp:temp:" + dsh), eq(res.getSecret()), any(Duration.class));
    }

    @Test
    void setupTotp_missingUser_shouldThrowException() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        Session session = Session.builder().dsh(dsh).build();

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(sessionManager.getMetadata(dsh, "username")).willReturn(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> totpService.setupTotp(dsh, tl));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void enableTotp_successfulBinding() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        String secret = TotpUtil.generateSecret();
        String validCode = TotpUtil.generateCode(secret, System.currentTimeMillis() / 1000L);

        TotpEnableRequestDTO req = TotpEnableRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(2)
                .code(validCode)
                .build();

        Session session = Session.builder().dsh(dsh).build();
        given(sessionManager.getSession(dsh)).willReturn(session);
        given(valueOperations.get("vredis:security:totp:temp:" + dsh)).willReturn(secret);
        given(sessionManager.getMetadata(dsh, "registeredUserId")).willReturn(1001L);

        User mockUser = User.builder().id(1001L).username("alice").build();
        given(userRepository.findById(1001L)).willReturn(Optional.of(mockUser));

        TotpEnableResponseVO res = totpService.enableTotp(req);

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.isSuccess());
        Assertions.assertEquals(secret, mockUser.getTotpSecret());
        Assertions.assertEquals(Boolean.TRUE, mockUser.getIsTotpEnabled());

        verify(transitionLockManager).validateTransLock(dsh, tl, 2, "pt/options");
        verify(userRepository).save(mockUser);
        verify(redisTemplate).delete("vredis:security:totp:temp:" + dsh);
        verify(sessionManager).setMetadata(dsh, "totpEnabled", true);
    }

    @Test
    void enableTotp_stringUserId_shouldParseSuccessfully() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        String secret = TotpUtil.generateSecret();
        String validCode = TotpUtil.generateCode(secret, System.currentTimeMillis() / 1000L);

        TotpEnableRequestDTO req = TotpEnableRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(2)
                .code(validCode)
                .build();

        Session session = Session.builder().dsh(dsh).build();
        given(sessionManager.getSession(dsh)).willReturn(session);
        given(valueOperations.get("vredis:security:totp:temp:" + dsh)).willReturn(secret);
        given(sessionManager.getMetadata(dsh, "registeredUserId")).willReturn("1002");

        User mockUser = User.builder().id(1002L).username("bob").build();
        given(userRepository.findById(1002L)).willReturn(Optional.of(mockUser));

        TotpEnableResponseVO res = totpService.enableTotp(req);

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.isSuccess());
        verify(userRepository).save(mockUser);
    }

    @Test
    void enableTotp_invalidCode_shouldThrowException() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        String secret = TotpUtil.generateSecret();

        TotpEnableRequestDTO req = TotpEnableRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(2)
                .code("000000")
                .build();

        Session session = Session.builder().dsh(dsh).build();
        given(sessionManager.getSession(dsh)).willReturn(session);
        given(valueOperations.get("vredis:security:totp:temp:" + dsh)).willReturn(secret);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> totpService.enableTotp(req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void enableTotp_expiredSession_shouldThrowException() {
        String dsh = "DSH_123";
        String tl = "TL_123";

        TotpEnableRequestDTO req = TotpEnableRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(2)
                .code("123456")
                .build();

        Session session = Session.builder().dsh(dsh).build();
        given(sessionManager.getSession(dsh)).willReturn(session);
        given(valueOperations.get("vredis:security:totp:temp:" + dsh)).willReturn(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> totpService.enableTotp(req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void verifyTotp_successful() {
        String dsh = "DSH_123";
        String tl = "TL_123";
        String secret = TotpUtil.generateSecret();
        String validCode = TotpUtil.generateCode(secret, System.currentTimeMillis() / 1000L);

        TOTPChallengeRequestDTO req = TOTPChallengeRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(1)
                .code(validCode)
                .build();

        SigninFlow flow = SigninFlow.create();
        flow.setCurrentStep("totp");
        flow.setIdentifier("alice");

        Session session = Session.builder().dsh(dsh).flow(flow).build();
        given(sessionManager.getSession(dsh)).willReturn(session);

        User mockUser = User.builder()
                .id(1001L)
                .username("alice")
                .isTotpEnabled(true)
                .totpSecret(secret)
                .build();
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(mockUser));
        given(transitionLockManager.nextTransLock(eq(dsh), eq(tl), eq(1), eq("totp"), eq(2), eq(SigninFlow.STEP_AUTHENTICATE), eq("alice")))
                .willReturn("NEXT_TL");

        FlowResponseVO res = totpService.verifyTotp(req);

        Assertions.assertNotNull(res);
        Assertions.assertEquals("NEXT_TL", res.getTl());
        Assertions.assertEquals(2, res.getCid());
        Assertions.assertEquals(SigninFlow.STEP_AUTHENTICATE, res.getNextStep());
        Assertions.assertEquals("CHALLENGE", res.getStatus());

        verify(transitionLockManager).validateTransLock(dsh, tl, "totp");
        verify(riskEvaluationEngine).recordAccountSuccess(eq("alice"), any());
    }

    @Test
    void verifyTotp_notEnabled_shouldThrowException() {
        String dsh = "DSH_123";
        String tl = "TL_123";

        TOTPChallengeRequestDTO req = TOTPChallengeRequestDTO.builder()
                .dsh(dsh)
                .tl(tl)
                .cid(1)
                .code("123456")
                .build();

        SigninFlow flow = SigninFlow.create();
        flow.setCurrentStep("totp");
        flow.setIdentifier("alice");

        Session session = Session.builder().dsh(dsh).flow(flow).build();
        given(sessionManager.getSession(dsh)).willReturn(session);

        User mockUser = User.builder()
                .id(1001L)
                .username("alice")
                .isTotpEnabled(false)
                .build();
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(mockUser));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> totpService.verifyTotp(req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void getTotpStatus_whenEnabled_returnsTrue() {
        User mockUser = User.builder()
                .id(1001L)
                .username("alice")
                .isTotpEnabled(true)
                .build();
        given(userRepository.findById(1001L)).willReturn(Optional.of(mockUser));

        TotpStatusVO status = totpService.getTotpStatus(1001L);

        Assertions.assertNotNull(status);
        Assertions.assertTrue(status.getEnabled());
    }

    @Test
    void getTotpStatus_whenNullUser_throwsUnauthorized() {
        Assertions.assertThrows(BusinessException.class, () -> totpService.getTotpStatus(null));
    }

    @Test
    void disableTotp_whenEnabled_disablesAndSaves() {
        User mockUser = User.builder()
                .id(1001L)
                .username("alice")
                .totpSecret("SECRET")
                .isTotpEnabled(true)
                .build();
        given(userRepository.findById(1001L)).willReturn(Optional.of(mockUser));

        totpService.disableTotp(1001L);

        Assertions.assertFalse(mockUser.getIsTotpEnabled());
        Assertions.assertNull(mockUser.getTotpSecret());
        verify(userRepository).save(mockUser);
    }

    @Test
    void disableTotp_whenNotEnabled_throwsBadRequest() {
        User mockUser = User.builder()
                .id(1001L)
                .username("alice")
                .isTotpEnabled(false)
                .build();
        given(userRepository.findById(1001L)).willReturn(Optional.of(mockUser));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> totpService.disableTotp(1001L));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void disableTotp_whenNullUser_throwsUnauthorized() {
        Assertions.assertThrows(BusinessException.class, () -> totpService.disableTotp(null));
    }
}
