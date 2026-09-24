package com.humolix.volur.auth.security.filter;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.admin.AdminRiskService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RiskIpFilterTest {

    private AdminRiskService adminRiskService;
    private SystemSettingService systemSettingService;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private HandlerExceptionResolver resolver;
    private FilterChain filterChain;
    private RiskIpFilter filter;

    @BeforeEach
    void setUp() {
        adminRiskService = mock(AdminRiskService.class);
        systemSettingService = mock(SystemSettingService.class);
        rateLimiter = mock(RedisSlidingWindowRateLimiter.class);
        resolver = mock(HandlerExceptionResolver.class);
        filterChain = mock(FilterChain.class);

        filter = new RiskIpFilter(adminRiskService, systemSettingService, rateLimiter, resolver);
    }

    @Test
    @DisplayName("OPTIONS 请求直接放行")
    void testOptionsRequestDirectlyPassed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(adminRiskService);
    }

    @Test
    @DisplayName("黑名单 IP 请求被即时拦截并抛出 403 Forbidden")
    void testBannedIpBlockedWith403() throws ServletException, IOException {
        String bannedIp = "192.168.1.100";
        when(adminRiskService.isIpBanned(bannedIp)).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.RISK_BAN_NOTICE_MESSAGE), anyString()))
                .thenReturn("您的 IP 存在异常访问行为，已被系统安全机制拦截。");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(bannedIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());

        Assertions.assertTrue(captor.getValue() instanceof BusinessException);
        BusinessException be = (BusinessException) captor.getValue();
        Assertions.assertEquals(HttpStatus.FORBIDDEN, be.getStatus());
    }

    @Test
    @DisplayName("正常 IP 在频控阈值内正常放行")
    void testAllowedIpWithinRateLimitPermitted() throws ServletException, IOException {
        String clientIp = "10.0.0.5";
        when(adminRiskService.isIpBanned(clientIp)).thenReturn(false);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true)).thenReturn(true);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true)).thenReturn(true);
        when(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, 60)).thenReturn(60);
        when(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS, 120)).thenReturn(120);
        when(rateLimiter.tryAcquire(anyString(), eq(120), any(Duration.class))).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("超出频控阈值的 IP 被拦截并抛出 429 Too Many Requests")
    void testRateLimitExceededBlocksWith429() throws ServletException, IOException {
        String clientIp = "10.0.0.5";
        when(adminRiskService.isIpBanned(clientIp)).thenReturn(false);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true)).thenReturn(true);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true)).thenReturn(true);
        when(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, 60)).thenReturn(60);
        when(systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS, 120)).thenReturn(120);
        when(rateLimiter.tryAcquire(anyString(), eq(120), any(Duration.class))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());

        Assertions.assertTrue(captor.getValue() instanceof BusinessException);
        BusinessException be = (BusinessException) captor.getValue();
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, be.getStatus());
    }

    @Test
    @DisplayName("本地回环 IP 豁免频控直接放行")
    void testLoopbackIpBypassesRateLimit() throws ServletException, IOException {
        String loopbackIp = "127.0.0.1";
        when(adminRiskService.isIpBanned(loopbackIp)).thenReturn(false);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true)).thenReturn(true);
        when(systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(loopbackIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(rateLimiter, never()).tryAcquire(anyString(), anyInt(), any(Duration.class));
    }

}
