package com.humolix.volur.security.session.validator;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.domain.Session;
import jakarta.servlet.http.Cookie;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class SessionDeviceBaselineValidatorTest {

    private SessionDeviceBaselineValidator validator;

    @BeforeEach
    void setUp() {
        RiskEvaluationEngine riskEvaluationEngine = new RiskEvaluationEngine(null, null, null, null);
        validator = new SessionDeviceBaselineValidator(riskEvaluationEngine);
    }

    @Test
    void validateBaseline_nullSession_shouldThrowUnauthorized() {
        Assertions.assertThrows(BusinessException.class, () -> validator.validateBaseline(null));
    }

    @Test
    void validateBaseline_matchingFingerprintAndIp_shouldPass() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.setCookies(new Cookie("_v_dtk", "fp_12345"));
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/130.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Session session = Session.builder()
                .requestDeviceParams(RequestDeviceParams.builder()
                        .ip("192.168.1.100")
                        .fingerprint("fp_12345")
                        .build())
                .requestHeader(RequestHeader.builder()
                        .userAgent("Mozilla/5.0 Chrome/130.0")
                        .build())
                .build();

        Assertions.assertDoesNotThrow(() -> validator.validateBaseline(session));
    }

    @Test
    void validateBaseline_mismatchedFingerprint_shouldThrowForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.setCookies(new Cookie("_v_dtk", "fp_attacker_999"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Session session = Session.builder()
                .requestDeviceParams(RequestDeviceParams.builder()
                        .ip("192.168.1.100")
                        .fingerprint("fp_12345")
                        .build())
                .build();

        Assertions.assertThrows(BusinessException.class, () -> validator.validateBaseline(session));
    }

    @Test
    void validateBaseline_ipChangedWithDifferentBrowserFamily_shouldThrowForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Firefox/131.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Session session = Session.builder()
                .requestDeviceParams(RequestDeviceParams.builder()
                        .ip("192.168.1.100")
                        .build())
                .requestHeader(RequestHeader.builder()
                        .userAgent("Mozilla/5.0 Chrome/130.0")
                        .build())
                .build();

        Assertions.assertThrows(BusinessException.class, () -> validator.validateBaseline(session));
    }

    @Test
    void validateBaseline_matchingBase64DeviceToken_shouldPass() {
        String tokenJson = "{\"did\":\"fe46497e-4fbe-41af-ab1e-4e142718c7bc\",\"dh\":\"16b4275b7d4cf6fc1cc0ab4da120bbed\",\"ch\":\"16b4275b7d4cf6fc\",\"bf\":\"CHROME\",\"hl\":0,\"ts\":1791239982045}";
        String base64Token = Base64.getEncoder().encodeToString(tokenJson.getBytes(StandardCharsets.UTF_8));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.setCookies(new Cookie("_v_dtk", base64Token), new Cookie("_v_did", "fe46497e-4fbe-41af-ab1e-4e142718c7bc"));
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/130.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Session session = Session.builder()
                .requestDeviceParams(RequestDeviceParams.builder()
                        .ip("192.168.1.100")
                        .fingerprint("16b4275b7d4cf6fc1cc0ab4da120bbed")
                        .deviceHash("16b4275b7d4cf6fc1cc0ab4da120bbed")
                        .deviceId("fe46497e-4fbe-41af-ab1e-4e142718c7bc")
                        .build())
                .requestHeader(RequestHeader.builder()
                        .userAgent("Mozilla/5.0 Chrome/130.0")
                        .build())
                .build();

        Assertions.assertDoesNotThrow(() -> validator.validateBaseline(session));
    }

    @Test
    void validateBaseline_matchingDeviceIdFallback_shouldPass() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.setCookies(new Cookie("_v_did", "fe46497e-4fbe-41af-ab1e-4e142718c7bc"));
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/130.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Session session = Session.builder()
                .requestDeviceParams(RequestDeviceParams.builder()
                        .ip("192.168.1.100")
                        .fingerprint("fe46497e4fbe41afab1e4e142718c7bc")
                        .deviceId("fe46497e-4fbe-41af-ab1e-4e142718c7bc")
                        .build())
                .requestHeader(RequestHeader.builder()
                        .userAgent("Mozilla/5.0 Chrome/130.0")
                        .build())
                .build();

        Assertions.assertDoesNotThrow(() -> validator.validateBaseline(session));
    }

}
