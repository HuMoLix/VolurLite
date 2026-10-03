package com.humolix.volur.auth.security.filter;

import com.humolix.volur.framework.common.exception.authException.csrfException.CsrfOriginDeniedException;
import com.humolix.volur.framework.common.exception.authException.csrfException.CsrfTokenMismatchException;
import com.humolix.volur.framework.common.util.RequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Value("${volur.security.cors.allowed-origins:}")
    private List<String> additionalAllowedOrigins;

    private final HandlerExceptionResolver resolver;

    public CsrfProtectionFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();
        if (SAFE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = RequestUtil.getCookieValue(request, "access_token");
        String refreshToken = RequestUtil.getCookieValue(request, "refresh_token");
        if (!StringUtils.hasText(accessToken) && !StringUtils.hasText(refreshToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String origin = request.getHeader("Origin");
            if (StringUtils.hasText(origin) && !RequestUtil.isOriginAllowed(origin, request, additionalAllowedOrigins)) {
                throw new CsrfOriginDeniedException();
            }

            String headerToken = request.getHeader("X-XSRF-TOKEN");
            if (!StringUtils.hasText(headerToken)) {
                headerToken = request.getHeader("X-CSRF-TOKEN");
            }
            String cookieToken = RequestUtil.getCookieValue(request, "XSRF-TOKEN");

            if (!StringUtils.hasText(cookieToken) || !StringUtils.hasText(headerToken) || !cookieToken.equals(headerToken)) {
                throw new CsrfTokenMismatchException();
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            resolver.resolveException(request, response, null, ex);
        }
    }

}
