package com.humolix.volur.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String VREDIS_AUTH_TOKEN_BLACKLIST_KEY_PREFIX = "vredis:auth:token:blacklist:";
    private static final String VREDIS_AUTH_USER_LOGOUT_KEY_PREFIX = "vredis:auth:user:logout:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthCookieService authCookieService;

    @Override
    public void blacklistToken(String token, long expirationMs) {
        long currentTime = System.currentTimeMillis();
        long ttl = expirationMs - currentTime;

        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    VREDIS_AUTH_TOKEN_BLACKLIST_KEY_PREFIX + token,
                    "blacklisted",
                    Duration.ofMillis(ttl)
            );
        }
    }

    @Override
    public void forceLogoutUser(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(
                VREDIS_AUTH_USER_LOGOUT_KEY_PREFIX + userId,
                timestamp,
                Duration.ofDays(7)
        );
    }

    @Override
    public boolean isUserForceLoggedOut(Long userId, long tokenIat) {
        String logoutTimestamp = redisTemplate.opsForValue().get(VREDIS_AUTH_USER_LOGOUT_KEY_PREFIX + userId);
        if (logoutTimestamp == null) {
            return false;
        }
        return tokenIat < Long.parseLong(logoutTimestamp);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VREDIS_AUTH_TOKEN_BLACKLIST_KEY_PREFIX + token));
    }

    @Override
    public void refreshToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请求上下文缺失");
        }
        String refreshToken = RequestUtil.getCookieValue(request, AuthCookieService.COOKIE_REFRESH_TOKEN);
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌缺失");
        }
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "无效或已过期的刷新令牌，请重新登录");
        }

        Jwt claims = jwtUtil.getClaimsFromToken(refreshToken);
        String oldJti = claims.getId();
        if (isTokenBlacklisted(oldJti)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "会话已注销，请重新登录");
        }

        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被封禁");
        }

        long iat = claims.getIssuedAt() != null ? claims.getIssuedAt().toEpochMilli() : 0L;
        if (isUserForceLoggedOut(user.getId(), iat)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "会话已失效，请重新登录");
        }

        Boolean rememberMeClaim = claims.getClaimAsBoolean("rememberMe");
        boolean rememberMe = Boolean.TRUE.equals(rememberMeClaim);

        long exp = claims.getExpiresAt() != null ? claims.getExpiresAt().toEpochMilli() : 0L;
        blacklistToken(oldJti, exp);

        String newJti = IdUtil.fastSimpleUUID();
        String newAccessToken = jwtUtil.generateAccessTokenFromUser(user, newJti);
        String newRefreshToken = jwtUtil.generateRefreshTokenFromUser(user, newJti, rememberMe);

        authCookieService.writeAuthCookies(newAccessToken, newRefreshToken, rememberMe);
    }

    @Override
    public void logout() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request != null) {
            String token = RequestUtil.getCookieValue(request, AuthCookieService.COOKIE_ACCESS_TOKEN);
            if (StringUtils.hasText(token)) {
                try {
                    Jwt claims = jwtUtil.getClaimsFromToken(token);
                    String jti = claims.getId();
                    if (!isTokenBlacklisted(jti)) {
                        long expiration = claims.getExpiresAt() != null ? claims.getExpiresAt().toEpochMilli() : 0L;
                        blacklistToken(jti, expiration);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        SecurityUtils.logout();
        authCookieService.clearAuthCookies();
    }

}
