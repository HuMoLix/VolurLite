package com.humolix.volur.auth.security;

import com.humolix.volur.auth.config.SecurityWhitelistProperties;
import com.humolix.volur.auth.security.util.SecurityWhitelistMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityWhitelistMatcherTest {

    @Test
    public void testPropertiesBindingFromEnvironment() {
        Map<String, String> properties = new HashMap<>();
        properties.put("volur.security.whitelist.additional-paths[0]", "/api/v1/custom/**");

        MapConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        SecurityWhitelistProperties bound = binder.bind("volur.security.whitelist", SecurityWhitelistProperties.class).get();
        Assertions.assertNotNull(bound);
        Assertions.assertTrue(bound.allPaths().contains("/api/v1/auth/logout"));
        Assertions.assertTrue(bound.allPaths().contains("/api/v1/custom/**"));
    }

    @Test
    public void testWhitelistMatcherRules() {
        SecurityWhitelistProperties properties = new SecurityWhitelistProperties(null, List.of("/api/v1/test/**"));
        SecurityWhitelistMatcher matcher = new SecurityWhitelistMatcher(properties);

        // 1. 验证关键的登出接口已放行
        Assertions.assertTrue(matcher.match("/api/v1/auth/logout"));

        // 2. 验证登录、注册、验证码、Flow 与 Signin 流转
        Assertions.assertTrue(matcher.match("/api/v1/auth/login"));
        Assertions.assertTrue(matcher.match("/api/v1/auth/signin/identifier"));
        Assertions.assertTrue(matcher.match("/api/v1/session/initSess"));
        Assertions.assertTrue(matcher.match("/api/v1/captcha/slide/verify"));

        // 3. 验证公开竞赛接口（精确路径与子路径）
        Assertions.assertTrue(matcher.match("/api/v1/competitions"));
        Assertions.assertTrue(matcher.match("/api/v1/competitions/123456"));

        // 4. 验证额外追加的路径
        Assertions.assertTrue(matcher.match("/api/v1/test/abc"));

        // 5. 验证受保护的接口不会被误判为白名单
        Assertions.assertFalse(matcher.match("/api/v1/user/profile"));
        Assertions.assertFalse(matcher.match("/api/v1/admin/users"));
        Assertions.assertFalse(matcher.match("/api/v1/admin/system/settings"));

        // 6. 验证 HttpServletRequest 匹配
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/logout");
        Assertions.assertTrue(matcher.match(request));
    }
}
