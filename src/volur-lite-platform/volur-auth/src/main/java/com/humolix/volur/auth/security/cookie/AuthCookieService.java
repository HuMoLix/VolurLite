package com.humolix.volur.auth.security.cookie;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.framework.common.api.ApiResponseCookie;
import com.humolix.volur.framework.common.api.CookieContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String COOKIE_ACCESS_TOKEN = "access_token";
    public static final String COOKIE_REFRESH_TOKEN = "refresh_token";
    public static final String COOKIE_LOGGED_IN = "logged_in";
    public static final String COOKIE_XSRF_TOKEN = "XSRF-TOKEN";
    public static final String COOKIE_ACCESS_EXP = "access_exp";

    private final JwtUtil jwtUtil;

    public boolean isSecure(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (StringUtils.hasText(proto) && "https".equalsIgnoreCase(proto.trim())) {
            return true;
        }
        String scheme = request.getHeader("X-Forwarded-Scheme");
        if (StringUtils.hasText(scheme) && "https".equalsIgnoreCase(scheme.trim())) {
            return true;
        }
        String ssl = request.getHeader("X-Forwarded-Ssl");
        return StringUtils.hasText(ssl) && "on".equalsIgnoreCase(ssl.trim());
    }

    public void writeAuthCookies(HttpServletRequest request, HttpServletResponse response, String accessToken, String refreshToken, boolean rememberMe) {
        boolean secure = isSecure(request);
        long accessExp = jwtUtil.getExpireSeconds() != null ? jwtUtil.getExpireSeconds() : 86400L;
        long refreshExpMs = rememberMe
                ? (jwtUtil.getRememberMeRefreshTokenExpirationMs() != null ? jwtUtil.getRememberMeRefreshTokenExpirationMs() : 2592000000L)
                : (jwtUtil.getRefreshTokenExpirationMs() != null ? jwtUtil.getRefreshTokenExpirationMs() : 604800000L);
        long refreshExp = refreshExpMs / 1000L;

        if (StringUtils.hasText(accessToken)) {
            ApiResponseCookie accessCookie = ApiResponseCookie.builder()
                    .name(COOKIE_ACCESS_TOKEN)
                    .value(accessToken)
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(Duration.ofSeconds(accessExp))
                    .build();
            CookieContextHolder.setCookie(accessCookie);
            if (response != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toResponseCookie().toString());
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            ApiResponseCookie refreshCookie = ApiResponseCookie.builder()
                    .name(COOKIE_REFRESH_TOKEN)
                    .value(refreshToken)
                    .httpOnly(true)
                    .secure(secure)
                    .path("/api/v1/auth/refresh")
                    .sameSite("Lax")
                    .maxAge(Duration.ofSeconds(refreshExp))
                    .build();
            CookieContextHolder.setCookie(refreshCookie);
            if (response != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toResponseCookie().toString());
            }
        }

        ApiResponseCookie loggedInCookie = ApiResponseCookie.builder()
                .name(COOKIE_LOGGED_IN)
                .value("true")
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(refreshExp))
                .build();
        CookieContextHolder.setCookie(loggedInCookie);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, loggedInCookie.toResponseCookie().toString());
        }

        String xsrfToken = IdUtil.fastSimpleUUID();
        ApiResponseCookie xsrfCookie = ApiResponseCookie.builder()
                .name(COOKIE_XSRF_TOKEN)
                .value(xsrfToken)
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(refreshExp))
                .build();
        CookieContextHolder.setCookie(xsrfCookie);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toResponseCookie().toString());
        }

        if (StringUtils.hasText(accessToken)) {
            long accessExpTimestampMs = System.currentTimeMillis() + (accessExp * 1000L);
            ApiResponseCookie accessExpCookie = ApiResponseCookie.builder()
                    .name(COOKIE_ACCESS_EXP)
                    .value(String.valueOf(accessExpTimestampMs))
                    .httpOnly(false)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(Duration.ofSeconds(accessExp))
                    .build();
            CookieContextHolder.setCookie(accessExpCookie);
            if (response != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, accessExpCookie.toResponseCookie().toString());
            }
        }
    }

    public void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = isSecure(request);

        ApiResponseCookie clearAccess = ApiResponseCookie.deletion(COOKIE_ACCESS_TOKEN, "/", secure);
        CookieContextHolder.setCookie(clearAccess);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toResponseCookie().toString());
        }

        ApiResponseCookie clearAccessExp = ApiResponseCookie.builder()
                .name(COOKIE_ACCESS_EXP)
                .value("")
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        CookieContextHolder.setCookie(clearAccessExp);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearAccessExp.toResponseCookie().toString());
        }

        ApiResponseCookie clearRefresh = ApiResponseCookie.deletion(COOKIE_REFRESH_TOKEN, "/api/v1/auth/refresh", secure);
        CookieContextHolder.setCookie(clearRefresh);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toResponseCookie().toString());
        }

        ApiResponseCookie clearLoggedIn = ApiResponseCookie.builder()
                .name(COOKIE_LOGGED_IN)
                .value("")
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        CookieContextHolder.setCookie(clearLoggedIn);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearLoggedIn.toResponseCookie().toString());
        }

        ApiResponseCookie clearXsrf = ApiResponseCookie.builder()
                .name(COOKIE_XSRF_TOKEN)
                .value("")
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        CookieContextHolder.setCookie(clearXsrf);
        if (response != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearXsrf.toResponseCookie().toString());
        }
    }

    public void writeAuthCookies(String accessToken, String refreshToken, boolean rememberMe) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpServletResponse response = attributes != null ? attributes.getResponse() : null;
        writeAuthCookies(request, response, accessToken, refreshToken, rememberMe);
    }

    public void clearAuthCookies() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpServletResponse response = attributes != null ? attributes.getResponse() : null;
        clearAuthCookies(request, response);
    }

}
