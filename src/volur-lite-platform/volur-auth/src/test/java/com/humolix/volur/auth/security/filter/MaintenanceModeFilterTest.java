package com.humolix.volur.auth.security.filter;

import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MaintenanceModeFilterTest {

    private SystemSettingService systemSettingService;
    private HandlerExceptionResolver resolver;
    private FilterChain filterChain;
    private MaintenanceModeFilter filter;

    @BeforeEach
    void setUp() {
        systemSettingService = mock(SystemSettingService.class);
        resolver = mock(HandlerExceptionResolver.class);
        filterChain = mock(FilterChain.class);
        filter = new MaintenanceModeFilter(systemSettingService, resolver);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("非维护模式下所有请求正常放行")
    void testMaintenanceDisabledPermitsAll() throws ServletException, IOException {
        when(systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("维护模式下白名单接口放行 (如 login)")
    void testMaintenanceEnabledPermitsExemptPaths() throws ServletException, IOException {
        when(systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("维护模式下注册接口被拦截并抛出 MaintenanceModeException 交由 resolver 处理")
    void testMaintenanceEnabledInterceptsRegister() throws ServletException, IOException {
        when(systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false)).thenReturn(true);
        when(systemSettingService.getSettingValue(eq(SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE), anyString()))
                .thenReturn("系统正在全站维护，暂不可用");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver, times(1)).resolveException(eq(request), eq(response), isNull(), captor.capture());

        Exception captured = captor.getValue();
        Assertions.assertInstanceOf(MaintenanceModeException.class, captured);
        Assertions.assertEquals("系统正在全站维护，暂不可用", captured.getMessage());
    }

    @Test
    @DisplayName("维护模式下已鉴权的管理员正常放行")
    void testMaintenanceEnabledPermitsAdmin() throws ServletException, IOException {
        when(systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false)).thenReturn(true);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/contests/manage");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }
}
