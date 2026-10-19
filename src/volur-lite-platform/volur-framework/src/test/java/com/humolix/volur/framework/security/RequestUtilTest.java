package com.humolix.volur.framework.security;

import com.humolix.volur.framework.common.util.RequestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

public class RequestUtilTest {

    @Test
    public void testExtractOriginHost() {
        Assertions.assertEquals("localhost", RequestUtil.extractOriginHost("http://localhost:3000"));
        Assertions.assertEquals("127.0.0.1", RequestUtil.extractOriginHost("http://127.0.0.1:5173"));
        Assertions.assertEquals("ctf.example.com", RequestUtil.extractOriginHost("https://ctf.example.com"));
        Assertions.assertEquals("ctf.example.com", RequestUtil.extractOriginHost("https://ctf.example.com:8443"));
        Assertions.assertEquals("::1", RequestUtil.extractOriginHost("http://[::1]:8080"));
        Assertions.assertNull(RequestUtil.extractOriginHost("null"));
        Assertions.assertNull(RequestUtil.extractOriginHost(""));
        Assertions.assertNull(RequestUtil.extractOriginHost(null));
    }

    @Test
    public void testExtractHost() {
        Assertions.assertEquals("example.com", RequestUtil.extractHost("example.com:8080"));
        Assertions.assertEquals("example.com", RequestUtil.extractHost("example.com"));
        Assertions.assertEquals("192.168.1.1", RequestUtil.extractHost("192.168.1.1:80"));
        Assertions.assertEquals("proxy1.com", RequestUtil.extractHost("proxy1.com, proxy2.com"));
        Assertions.assertEquals("::1", RequestUtil.extractHost("[::1]:8080"));
        Assertions.assertEquals("::1", RequestUtil.extractHost("[::1]"));
        Assertions.assertNull(RequestUtil.extractHost(null));
        Assertions.assertNull(RequestUtil.extractHost(""));
    }

    @Test
    public void testExtractRequestHost() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "192.168.1.50:80");
        Assertions.assertEquals("192.168.1.50", RequestUtil.extractRequestHost(request));

        // X-Forwarded-Host 优先级高于 Host
        request.addHeader("X-Forwarded-Host", "ctf.domain.com:443");
        Assertions.assertEquals("ctf.domain.com", RequestUtil.extractRequestHost(request));
    }

    @Test
    public void testIsOriginAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "ctf.domain.com");

        // 1. 本地开发放行
        Assertions.assertTrue(RequestUtil.isOriginAllowed("http://localhost:3000", request, null));
        Assertions.assertTrue(RequestUtil.isOriginAllowed("http://127.0.0.1:5173", request, null));

        // 2. 同 Host 生产自适应放行
        Assertions.assertTrue(RequestUtil.isOriginAllowed("https://ctf.domain.com", request, null));
        Assertions.assertTrue(RequestUtil.isOriginAllowed("http://ctf.domain.com:8080", request, null));

        // 3. 外部第三方恶意域拦截
        Assertions.assertFalse(RequestUtil.isOriginAllowed("https://evil.attacker.com", request, null));
        Assertions.assertFalse(RequestUtil.isOriginAllowed("null", request, null));

        // 4. 白名单追加放行
        Assertions.assertTrue(RequestUtil.isOriginAllowed("https://portal.domain.com", request, List.of("https://portal.domain.com")));
    }

    @Test
    public void testGetCookieValueAndDeviceFingerprint() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1. 无 Cookie 时应返回 null
        Assertions.assertNull(RequestUtil.getDeviceFingerprint(request));

        // 2. 存在 Header X-Device-Fingerprint 但无 Cookie 时，必须返回 null (验证已彻底废除 Header)
        request.addHeader("X-Device-Fingerprint", "legacy_header_token");
        Assertions.assertNull(RequestUtil.getDeviceFingerprint(request));

        // 3. 存在通用 fingerprint Cookie 时正确读取
        request.setCookies(new jakarta.servlet.http.Cookie("fingerprint", "universal_fp_val"));
        Assertions.assertEquals("universal_fp_val", RequestUtil.getDeviceFingerprint(request));

        // 4. 存在长效设备 ID (_v_did) 时，优先级高于通用 fingerprint
        request.setCookies(
                new jakarta.servlet.http.Cookie("fingerprint", "universal_fp_val"),
                new jakarta.servlet.http.Cookie("_v_did", "device_id_uuid_123")
        );
        Assertions.assertEquals("device_id_uuid_123", RequestUtil.getDeviceFingerprint(request));

        // 5. 存在动态风控凭证 (_v_dtk) 时，最高优先级
        request.setCookies(
                new jakarta.servlet.http.Cookie("fingerprint", "universal_fp_val"),
                new jakarta.servlet.http.Cookie("_v_did", "device_id_uuid_123"),
                new jakarta.servlet.http.Cookie("_v_dtk", "device_risk_token_456")
        );
        Assertions.assertEquals("device_risk_token_456", RequestUtil.getDeviceFingerprint(request));
    }

}
