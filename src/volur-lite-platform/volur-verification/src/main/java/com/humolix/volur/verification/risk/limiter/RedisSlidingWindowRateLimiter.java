package com.humolix.volur.verification.risk.limiter;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSlidingWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String key, int maxRequests, Duration window) {
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
            log.error("[Volur-Verification] 滑动窗口限频执行异常: key={}", key, ex);
            return true;
        }
    }

    public long getCurrentCount(String key, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            Long count = redisTemplate.opsForZSet().zCard(key);
            return count != null ? count : 0L;
        } catch (Exception ex) {
            log.error("[Volur-Verification] 获取限频计数异常: key={}", key, ex);
            return 0L;
        }
    }

    public void reduceCount(String key, long count) {
        if (count <= 0) {
            return;
        }
        try {
            redisTemplate.opsForZSet().removeRange(key, 0, count - 1);
        } catch (Exception ex) {
            log.error("[Volur-Verification] 扣减限频/风险计数异常: key={}, count={}", key, count, ex);
        }
    }

}
