package com.humolix.volur.auth.security;

import com.humolix.volur.auth.config.SecurityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

public class SecurityConfigCorsTest {

    private SecurityConfig createSecurityConfig(List<String> additionalOrigins) {
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, null, null, null);
        if (additionalOrigins != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(config, "additionalAllowedOrigins", additionalOrigins);
        }
        return config;
    }

    @Test
    public void testNonCorsRequestReturnsNull() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        // 没有 Origin 请求头
        Assertions.assertNull(source.getCorsConfiguration(request));

        // Origin 为空串
        request.addHeader("Origin", "");
        Assertions.assertNull(source.getCorsConfiguration(request));
    }

    @Test
    public void testLocalhostDevelopmentAllowed() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        // 1. 本地 Vite 端口 3000
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Host", "192.168.1.100:8080");

        CorsConfiguration cors = source.getCorsConfiguration(request);
        Assertions.assertNotNull(cors);
        Assertions.assertEquals(List.of("http://localhost:3000"), cors.getAllowedOrigins());
        Assertions.assertFalse(cors.getAllowCredentials());

        // 2. 本地 127.0.0.1
        MockHttpServletRequest request127 = new MockHttpServletRequest();
        request127.addHeader("Origin", "http://127.0.0.1:5173");
        request127.addHeader("Host", "ctf.domain.com");

        CorsConfiguration cors127 = source.getCorsConfiguration(request127);
        Assertions.assertNotNull(cors127);
        Assertions.assertEquals(List.of("http://127.0.0.1:5173"), cors127.getAllowedOrigins());
    }

    @Test
    public void testSameHostAdaptiveAllowed() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        // 1. 用户使用 IP 访问场景
        MockHttpServletRequest requestIp = new MockHttpServletRequest();
        requestIp.addHeader("Origin", "http://192.168.1.200");
        requestIp.addHeader("Host", "192.168.1.200:80");

        CorsConfiguration corsIp = source.getCorsConfiguration(requestIp);
        Assertions.assertNotNull(corsIp);
        Assertions.assertEquals(List.of("http://192.168.1.200"), corsIp.getAllowedOrigins());
        Assertions.assertFalse(corsIp.getAllowCredentials());

        // 2. 用户绑定自定义域名场景 (Host 透传)
        MockHttpServletRequest requestDomain = new MockHttpServletRequest();
        requestDomain.addHeader("Origin", "https://ctf.myteam.com");
        requestDomain.addHeader("Host", "ctf.myteam.com");

        CorsConfiguration corsDomain = source.getCorsConfiguration(requestDomain);
        Assertions.assertNotNull(corsDomain);
        Assertions.assertEquals(List.of("https://ctf.myteam.com"), corsDomain.getAllowedOrigins());

        // 3. 用户绑定自定义域名场景 (Nginx X-Forwarded-Host 透传)
        MockHttpServletRequest requestForwarded = new MockHttpServletRequest();
        requestForwarded.addHeader("Origin", "https://game.security.org");
        requestForwarded.addHeader("Host", "volur-app:8080"); // 内部服务名
        requestForwarded.addHeader("X-Forwarded-Host", "game.security.org"); // 反代透传的真实域名

        CorsConfiguration corsForwarded = source.getCorsConfiguration(requestForwarded);
        Assertions.assertNotNull(corsForwarded);
        Assertions.assertEquals(List.of("https://game.security.org"), corsForwarded.getAllowedOrigins());
    }

    @Test
    public void testThirdPartyMaliciousOriginRejected() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        // 攻击者站点尝试跨域探测用户的内网 IP 或域名
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.attacker.com");
        request.addHeader("Host", "192.168.1.200");

        CorsConfiguration cors = source.getCorsConfiguration(request);
        Assertions.assertNull(cors, "来自第三方未授权源 evil.attacker.com 的请求必须被直接拦截拒绝");

        // 攻击者站点尝试使用 null origin
        MockHttpServletRequest requestNull = new MockHttpServletRequest();
        requestNull.addHeader("Origin", "null");
        requestNull.addHeader("Host", "ctf.domain.com");

        Assertions.assertNull(source.getCorsConfiguration(requestNull), "null origin 必须被直接拦截拒绝");
    }

    @Test
    public void testAdditionalAllowedOrigins() {
        SecurityConfig config = createSecurityConfig(List.of("https://portal.mycompany.com"));
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://portal.mycompany.com");
        request.addHeader("Host", "ctf.domain.com");

        CorsConfiguration cors = source.getCorsConfiguration(request);
        Assertions.assertNotNull(cors);
        Assertions.assertEquals(List.of("https://portal.mycompany.com"), cors.getAllowedOrigins());
    }

}
