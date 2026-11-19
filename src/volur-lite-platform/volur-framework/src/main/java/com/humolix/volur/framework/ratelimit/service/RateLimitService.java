package com.humolix.volur.framework.ratelimit.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    public static final String VREDIS_RATELIMIT_SLIDING_KEY_PREFIX = "vredis:ratelimit:sliding:";
    public static final String VREDIS_RATELIMIT_FIXED_KEY_PREFIX = "vredis:ratelimit:fixed:";
    public static final String VREDIS_RATELIMIT_BRUTE_FAIL_KEY_PREFIX = "vredis:ratelimit:brute:fail:";
    public static final String VREDIS_RATELIMIT_BRUTE_LOCK_KEY_PREFIX = "vredis:ratelimit:brute:lock:";

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquireSliding(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count != null && count >= maxRequests) {
                return false;
            }
            String member = now + "-" + IdUtil.fastSimpleUUID();
            redisTemplate.opsForZSet().add(key, member, now);
            redisTemplate.expire(key, window.plusMinutes(1));
            return true;
        } catch (Exception ex) {
            log.error("[RateLimit] Sliding window execution error: key={}", key, ex);
            return true;
        }
    }

    public boolean tryAcquireFixed(String key, int maxRequests, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            Long expire = redisTemplate.getExpire(key);
            if (count != null && (count == 1 || expire == null || expire == -1)) {
                redisTemplate.expire(key, window);
            }
            return count != null && count <= maxRequests;
        } catch (Exception ex) {
            log.error("[RateLimit] Fixed window execution error: key={}", key, ex);
            return true;
        }
    }

    public boolean isBruteForceLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception ex) {
            log.error("[RateLimit] Check brute force lock error: lockKey={}", lockKey, ex);
            return false;
        }
    }

    public long getBruteForceLockRemainingSeconds(String lockKey) {
        try {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            return (ttl != null && ttl > 0) ? ttl : 0L;
        } catch (Exception ex) {
            log.error("[RateLimit] Get lock ttl error: lockKey={}", lockKey, ex);
            return 0L;
        }
    }

    public void recordBruteForceSuccess(String failKey) {
        try {
            redisTemplate.delete(failKey);
        } catch (Exception ex) {
            log.error("[RateLimit] Clear brute force failure error: failKey={}", failKey, ex);
        }
    }

    public boolean recordBruteForceFailure(String failKey, String lockKey, int maxFailures, Duration window, Duration lockDuration) {
        try {
            Long count = redisTemplate.opsForValue().increment(failKey);
            Long expire = redisTemplate.getExpire(failKey);
            if (count != null && (count == 1 || expire == null || expire == -1)) {
                redisTemplate.expire(failKey, window);
            }
            if (count != null && count >= maxFailures) {
                redisTemplate.opsForValue().set(lockKey, "1", lockDuration);
                redisTemplate.delete(failKey);
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.error("[RateLimit] Record brute force failure error: failKey={}, lockKey={}", failKey, lockKey, ex);
            return false;
        }
    }
}
