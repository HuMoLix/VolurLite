package com.humolix.volur.auth.security.filter;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 全站维护模式全局过滤器
 * 当开启全站维护时，仅放行公开设置接口、管理员登录接口、人机验证以及具有 ADMIN 权限的管理端请求，其余一律拦截并返回 503 与维护公告内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXEMPT_PATHS = List.of(
            "/api/v1/system/settings/public",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/captcha/**",
            "/api/v1/verification/**",
            "/api/v1/uploads/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/error",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isMaintenance = systemSettingService.getBoolean("system.maintenance.enabled", false);
        if (!isMaintenance) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();

        // 1. 检查是否在白名单放行路径中
        for (String pattern : EXEMPT_PATHS) {
            if (pathMatcher.match(pattern, requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 2. 检查是否为已鉴权的管理员
        if (SecurityUtils.isAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 拦截普通用户与匿名访问，返回 503 状态码及维护通知
        String notice = systemSettingService.getSettingValue("system.maintenance.notice_message", "系统正在全站维护升级中，暂不可用，请稍后访问。");
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        var apiResponse = ApiResponseEntity.error(HttpStatus.SERVICE_UNAVAILABLE, notice);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
