package com.humolix.volur.auth.security.filter;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.framework.common.exception.MaintenanceModeException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

/**
 * 全站维护模式全局过滤器
 * 当开启全站维护时，仅放行公开设置接口、管理员登录接口、人机验证以及具有 ADMIN 权限的管理端请求，
 * 其余一律通过 HandlerExceptionResolver 抛出 MaintenanceModeException，由 GlobalExceptionHandler 统一返回 503 与维护公告。
 */
@Slf4j
@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemSettingService systemSettingService;
    private final HandlerExceptionResolver resolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public MaintenanceModeFilter(
            SystemSettingService systemSettingService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.systemSettingService = systemSettingService;
        this.resolver = resolver;
    }

    private static final List<String> EXEMPT_PATHS = List.of(
            "/api/v1/system/settings/public",
            "/api/v1/auth/login",
            "/api/v1/auth/signin/**",
            "/api/v1/session/**",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/captcha/**",
            "/api/v1/verification/**",
            "/api/v1/uploads/**",
            "/error",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isMaintenance = systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false);
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

        // 3. 拦截普通用户与匿名访问，委托 HandlerExceptionResolver 触发 MaintenanceModeException，交由 GlobalExceptionHandler 统一处理
        String notice = systemSettingService.getSettingValue(
                SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE,
                "系统正在全站维护升级中，暂不可用，请稍后访问。"
        );
        resolver.resolveException(request, response, null, new MaintenanceModeException(notice));
    }
}
