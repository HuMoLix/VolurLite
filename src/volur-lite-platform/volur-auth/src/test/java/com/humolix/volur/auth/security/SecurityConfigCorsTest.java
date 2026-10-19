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
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, null, null, null, null);
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
        Assertions.assertNull(source.getCorsConfiguration(request));

        request.addHeader("Origin", "");
        Assertions.assertNull(source.getCorsConfiguration(request));
    }

    @Test
    public void testLocalhostDevelopmentAllowed() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Host", "192.168.1.100:8080");

        CorsConfiguration cors = source.getCorsConfiguration(request);
        Assertions.assertNotNull(cors);
        Assertions.assertEquals(List.of("http://localhost:3000"), cors.getAllowedOrigins());
        Assertions.assertTrue(cors.getAllowCredentials());

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

        MockHttpServletRequest requestIp = new MockHttpServletRequest();
        requestIp.addHeader("Origin", "http://192.168.1.200");
        requestIp.addHeader("Host", "192.168.1.200:80");

        CorsConfiguration corsIp = source.getCorsConfiguration(requestIp);
        Assertions.assertNotNull(corsIp);
        Assertions.assertEquals(List.of("http://192.168.1.200"), corsIp.getAllowedOrigins());
        Assertions.assertTrue(corsIp.getAllowCredentials());

        MockHttpServletRequest requestDomain = new MockHttpServletRequest();
        requestDomain.addHeader("Origin", "https://ctf.myteam.com");
        requestDomain.addHeader("Host", "ctf.myteam.com");

        CorsConfiguration corsDomain = source.getCorsConfiguration(requestDomain);
        Assertions.assertNotNull(corsDomain);
        Assertions.assertEquals(List.of("https://ctf.myteam.com"), corsDomain.getAllowedOrigins());

        MockHttpServletRequest requestForwarded = new MockHttpServletRequest();
        requestForwarded.addHeader("Origin", "https://game.security.org");
        requestForwarded.addHeader("Host", "volur-app:8080");
        requestForwarded.addHeader("X-Forwarded-Host", "game.security.org");

        CorsConfiguration corsForwarded = source.getCorsConfiguration(requestForwarded);
        Assertions.assertNotNull(corsForwarded);
        Assertions.assertEquals(List.of("https://game.security.org"), corsForwarded.getAllowedOrigins());
    }

    @Test
    public void testThirdPartyMaliciousOriginRejected() {
        SecurityConfig config = createSecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.attacker.com");
        request.addHeader("Host", "192.168.1.200");

        CorsConfiguration cors = source.getCorsConfiguration(request);
        Assertions.assertNull(cors);

        MockHttpServletRequest requestNull = new MockHttpServletRequest();
        requestNull.addHeader("Origin", "null");
        requestNull.addHeader("Host", "ctf.domain.com");

        Assertions.assertNull(source.getCorsConfiguration(requestNull));
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
