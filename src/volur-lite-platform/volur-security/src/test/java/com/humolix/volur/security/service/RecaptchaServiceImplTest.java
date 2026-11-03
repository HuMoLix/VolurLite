package com.humolix.volur.security.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.service.impl.RecaptchaServiceImpl;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.service.CaptchaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CaptchaCryptoService captchaCryptoService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private RiskEvaluationEngine riskEvaluationEngine;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private TransitionLockManager transitionLockManager;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks
    private RecaptchaServiceImpl recaptchaService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void getRecaptchaChallenge_lowRisk_returnsClickChallenge() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";

        Session session = Session.builder()
                .dsh(dsh)
                .flow(Flow.builder().identifier("testuser").build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_123"))
                .riskLevel("LOW")
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.LOW);
        given(captchaCryptoService.getPublicKeyPem()).willReturn("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A");

        RecaptchaChallengeVO result = recaptchaService.getRecaptchaChallenge(dsh, tl);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("CLICK", result.getChallengeType());
        Assertions.assertNotNull(result.getRecaptchaToken());
        Assertions.assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A", result.getPublicKey());
        verify(valueOperations).set(startsWith("vredis:security:recaptcha:challenge:"), anyString(), any(Duration.class));
    }

    @Test
    void getRecaptchaChallenge_highRisk_returnsPuzzleChallenge() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";

        Session session = Session.builder()
                .dsh(dsh)
                .flow(Flow.builder().identifier("testuser").build())
                .requestDeviceParams(RequestDeviceParams.of("127.0.0.1", "fp_123"))
                .riskLevel("HIGH")
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.HIGH);
        given(captchaCryptoService.getPublicKeyPem()).willReturn("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A");

        CaptchaResponseVO puzzleVO = CaptchaResponseVO.builder()
                .captchaId("inner_cap_123")
                .captchaType(CaptchaType.SLIDE_PUZZLE.name())
                .imageBase64("bg_base64")
                .sliderBase64("slider_base64")
                .yPosition(55)
                .build();
        given(captchaService.generateChallenge(eq("LOGIN"), eq(CaptchaType.SLIDE_PUZZLE.name()), isNull())).willReturn(puzzleVO);

        RecaptchaChallengeVO result = recaptchaService.getRecaptchaChallenge(dsh, tl);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("PUZZLE", result.getChallengeType());
        Assertions.assertEquals("bg_base64", result.getBackgroundImage());
        Assertions.assertEquals("slider_base64", result.getPuzzleImage());
        Assertions.assertEquals(55, result.getPuzzleY());
    }

    @Test
    void verifyRecaptcha_clickMode_validHuman_passesAndIssuesTicket() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{\"ts\":" + System.currentTimeMillis() + ",\"isTrusted\":true,\"bio\":{\"pts\":15,\"dur\":80,\"accVar\":0.005,\"inActionBounds\":true},\"env\":{\"wd\":false,\"cdp\":false,\"coreHook\":false}}";
        given(captchaCryptoService.decryptPayload("mock_payload_base64")).willReturn(mockDecryptedPayload);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload_base64");
        RecaptchaVerifyResponseVO response = recaptchaService.verifyRecaptcha(dsh, tl, requestDTO);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isPassed());
        Assertions.assertNotNull(response.getRecaptchaTicket());
        Assertions.assertTrue(response.getRecaptchaTicket().startsWith("rctk_"));
        verify(redisTemplate).delete("vredis:security:recaptcha:challenge:" + token);
    }

    @Test
    void verifyRecaptcha_clickMode_realUserPayloadWithExtensions_passes() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String userPayload = "{\"v\":4,\"bio\":{\"accVar\":0.1454683864799204,\"curvVar\":0.40842813002096506,\"smooth\":0.9867001244196184,\"pts\":80,\"dur\":125,\"downUpDur\":67,\"radius\":8.06},\"env\":{\"wd\":false,\"cdp\":false,\"hook\":true,\"gpu\":\"ANGLE (NVIDIA, NVIDIA GeForce RT\",\"cores\":28,\"mem\":32},\"eco\":{\"hist\":24,\"usage\":0,\"idb\":0},\"ts\":" + System.currentTimeMillis() + ",\"nonce\":\"lpf7wkgv\",\"powNonce\":\"\"}";
        given(captchaCryptoService.decryptPayload("user_real_payload")).willReturn(userPayload);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "user_real_payload");
        RecaptchaVerifyResponseVO response = recaptchaService.verifyRecaptcha(dsh, tl, requestDTO);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isPassed());
        Assertions.assertNotNull(response.getRecaptchaTicket());
        Assertions.assertTrue(response.getRecaptchaTicket().startsWith("rctk_"));
    }


    @Test
    void verifyRecaptcha_clickMode_untrustedEvent_fails() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{\"ts\":" + System.currentTimeMillis() + ",\"isTrusted\":false,\"bio\":{\"pts\":15,\"dur\":80}}";
        given(captchaCryptoService.decryptPayload("mock_payload_base64")).willReturn(mockDecryptedPayload);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload_base64");
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> recaptchaService.verifyRecaptcha(dsh, tl, requestDTO));
        Assertions.assertEquals("行为模式与运行环境存在异常，人机验证失败", ex.getMessage());
    }

    @Test
    void verifyRecaptcha_clickMode_webdriver_fails() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{\"ts\":" + System.currentTimeMillis() + ",\"isTrusted\":true,\"env\":{\"wd\":true}}";
        given(captchaCryptoService.decryptPayload("mock_payload_base64")).willReturn(mockDecryptedPayload);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload_base64");
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> recaptchaService.verifyRecaptcha(dsh, tl, requestDTO));
        Assertions.assertEquals("行为模式与运行环境存在异常，人机验证失败", ex.getMessage());
    }

    @Test
    void verifyRecaptcha_clickMode_emptyPayload_fails() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{}";
        given(captchaCryptoService.decryptPayload("mock_payload_base64")).willReturn(mockDecryptedPayload);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload_base64");
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> recaptchaService.verifyRecaptcha(dsh, tl, requestDTO));
        Assertions.assertEquals("人机验证凭据已过期或时间戳非法", ex.getMessage());
    }

    @Test
    void verifyRecaptcha_puzzleMode_success_returnsTicket() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"PUZZLE\",\"captchaId\":\"inner_cap_123\",\"createdAt\":" + System.currentTimeMillis() + "}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{\"ts\":" + System.currentTimeMillis() + ",\"x\":140}";
        given(captchaCryptoService.decryptPayload("mock_payload_base64")).willReturn(mockDecryptedPayload);

        CaptchaTicketVO ticketVO = CaptchaTicketVO.builder()
                .ticket("ticket_123")
                .scene("LOGIN")
                .expireSeconds(300L)
                .build();
        given(captchaService.verifyAndIssueTicket(any())).willReturn(ticketVO);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload_base64");
        RecaptchaVerifyResponseVO response = recaptchaService.verifyRecaptcha(dsh, tl, requestDTO);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isPassed());
        Assertions.assertNotNull(response.getRecaptchaTicket());
        Assertions.assertTrue(response.getRecaptchaTicket().startsWith("rctk_"));
    }

    @Test
    void verifyRecaptcha_expiredChallenge_throwsException() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_expired";

        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(null);

        RecaptchaVerifyRequestDTO requestDTO = new RecaptchaVerifyRequestDTO(token, "mock_payload");
        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> recaptchaService.verifyRecaptcha(dsh, tl, requestDTO));
        Assertions.assertEquals("人机验证挑战已失效，请重新验证", ex.getMessage());
    }

    @Test
    void validateAndBurnTicket_valid_returnsTrueAndBurns() {
        String dsh = "S-100200:1790000";
        String ticket = "rctk_sample_valid";

        given(valueOperations.get("vredis:security:recaptcha:ticket:" + ticket)).willReturn(dsh);

        boolean valid = recaptchaService.validateAndBurnTicket(dsh, ticket);
        Assertions.assertTrue(valid);
        verify(redisTemplate).delete("vredis:security:recaptcha:ticket:" + ticket);
    }

    @Test
    void validateAndBurnTicket_invalid_returnsFalse() {
        String dsh = "S-100200:1790000";
        String ticket = "rctk_invalid";

        given(valueOperations.get("vredis:security:recaptcha:ticket:" + ticket)).willReturn(null);

        boolean valid = recaptchaService.validateAndBurnTicket(dsh, ticket);
        Assertions.assertFalse(valid);
    }

}
