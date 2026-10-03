package com.humolix.volur.security.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.impl.RecaptchaServiceImpl;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.service.CaptchaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class RecaptchaServiceImplTest {

    private TransitionLockManager transitionLockManager;
    private RiskEvaluationEngine riskEvaluationEngine;
    private CaptchaCryptoService captchaCryptoService;
    private CaptchaService captchaService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private final ObjectMapper objectMapper = new JsonMapper();

    private RecaptchaServiceImpl recaptchaService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        transitionLockManager = Mockito.mock(TransitionLockManager.class);
        riskEvaluationEngine = Mockito.mock(RiskEvaluationEngine.class);
        captchaCryptoService = Mockito.mock(CaptchaCryptoService.class);
        captchaService = Mockito.mock(CaptchaService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        recaptchaService = new RecaptchaServiceImpl(
                transitionLockManager,
                riskEvaluationEngine,
                captchaCryptoService,
                captchaService,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    void getRecaptchaChallenge_lowRisk_returnsClickChallenge() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .identifier("testuser")
                .deviceFingerprint("fp_123")
                .riskLevel("LOW")
                .build();

        given(transitionLockManager.getSession(dsh)).willReturn(session);
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

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .identifier("testuser")
                .deviceFingerprint("fp_123")
                .riskLevel("HIGH")
                .build();

        given(transitionLockManager.getSession(dsh)).willReturn(session);
        given(riskEvaluationEngine.evaluateComprehensiveRisk(any(), any(), any(), any())).willReturn(RiskLevel.HIGH);
        given(captchaCryptoService.getPublicKeyPem()).willReturn("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A");

        CaptchaResponseVO puzzleVO = CaptchaResponseVO.builder()
                .captchaId("inner_cap_123")
                .captchaType("SLIDER")
                .imageBase64("bg_base64")
                .sliderBase64("slider_base64")
                .yPosition(55)
                .build();
        given(captchaService.generateChallenge(eq("LOGIN"), eq("SLIDER"), isNull())).willReturn(puzzleVO);

        RecaptchaChallengeVO result = recaptchaService.getRecaptchaChallenge(dsh, tl);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("PUZZLE", result.getChallengeType());
        Assertions.assertEquals("bg_base64", result.getBackgroundImage());
        Assertions.assertEquals("slider_base64", result.getPuzzleImage());
        Assertions.assertEquals(55, result.getPuzzleY());
    }

    @Test
    void verifyRecaptcha_clickMode_success_returnsTicket() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"CLICK\",\"createdAt\":1790000}";
        given(valueOperations.get("vredis:security:recaptcha:challenge:" + token)).willReturn(contextJson);

        String mockDecryptedPayload = "{\"ts\":" + System.currentTimeMillis() + ",\"webdriver\":false,\"isTrusted\":true}";
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
    void verifyRecaptcha_puzzleMode_success_returnsTicket() {
        String dsh = "S-100200:1790000";
        String tl = "ADG-RECAPTCHA";
        String token = "rctok_123456";

        String contextJson = "{\"dsh\":\"" + dsh + "\",\"challengeType\":\"PUZZLE\",\"captchaId\":\"inner_cap_123\",\"createdAt\":1790000}";
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
