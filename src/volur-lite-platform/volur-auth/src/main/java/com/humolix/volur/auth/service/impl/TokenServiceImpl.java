package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String VREDIS_AUTH_TOKEN_BLACKLIST_KEY_PREFIX = "vredis:auth:token:blacklist:";
    private static final String VREDIS_AUTH_USER_LOGOUT_KEY_PREFIX = "vredis:auth:user:logout:";

    private final StringRedisTemplate redisTemplate;

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

}
