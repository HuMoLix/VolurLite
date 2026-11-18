package com.humolix.volur.training.service;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.training.constant.TrainingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.humolix.volur.training.constant.TrainingConstants.EC_FLAG_COOLDOWN;
import static com.humolix.volur.training.constant.TrainingConstants.EC_FLAG_GLOBAL_RATE_LIMITED;
import static com.humolix.volur.training.constant.TrainingConstants.EC_FLAG_LOCKED;

/**
 * flag 提交限流（Redis 固定窗口）：
 * 全局每 2 秒 1 次 → 单题答错冷却 5 秒 → 同题连错 10 次锁题 5 分钟。
 * Redis 异常时放行（fail-open），与平台既有限流组件口径一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingRateLimitService {

    private final StringRedisTemplate redis;

    /** 提交前依次调用：锁题 → 冷却 → 全局限速 */
    public void checkBeforeSubmit(Long userId, Long sourceChallengeId) {
        checkLocked(userId, sourceChallengeId);
        checkCooldown(userId, sourceChallengeId);
        checkGlobalSubmit(userId);
    }

    /** 答错后调用：写冷却标记并累计错误数，达到阈值时写锁定标记（不抛异常，本次仍返回 WRONG） */
    public void recordWrongSubmit(Long userId, Long sourceChallengeId) {
        try {
            redis.opsForValue().set(
                    TrainingConstants.KEY_FLAG_COOLDOWN + userId + ":" + sourceChallengeId,
                    "1", TrainingConstants.FLAG_COOLDOWN);

            String wrongKey = TrainingConstants.KEY_FLAG_WRONG + userId + ":" + sourceChallengeId;
            Long count = redis.opsForValue().increment(wrongKey);
            if (count != null && count == 1L) {
                redis.expire(wrongKey, TrainingConstants.FLAG_WRONG_COUNT_WINDOW);
            }
            if (count != null && count >= TrainingConstants.FLAG_WRONG_LOCK_THRESHOLD) {
                redis.opsForValue().set(
                        TrainingConstants.KEY_FLAG_LOCK + userId + ":" + sourceChallengeId,
                        "1", TrainingConstants.FLAG_LOCK_DURATION);
                redis.delete(wrongKey);
            }
        } catch (Exception ex) {
            log.error("[Volur-Training] 记录错误提交限流标记异常: userId={}, challengeId={}", userId, sourceChallengeId, ex);
        }
    }

    private void checkLocked(Long userId, Long sourceChallengeId) {
        try {
            String lockKey = TrainingConstants.KEY_FLAG_LOCK + userId + ":" + sourceChallengeId;
            if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
                long seconds = remainingSeconds(lockKey, TrainingConstants.FLAG_LOCK_DURATION);
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, EC_FLAG_LOCKED,
                        "该题连续错误次数过多已锁定，请约 " + formatSeconds(seconds) + " 后再试");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[Volur-Training] 锁题校验异常: userId={}, challengeId={}", userId, sourceChallengeId, ex);
        }
    }

    private void checkCooldown(Long userId, Long sourceChallengeId) {
        try {
            String cooldownKey = TrainingConstants.KEY_FLAG_COOLDOWN + userId + ":" + sourceChallengeId;
            if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
                long seconds = remainingSeconds(cooldownKey, TrainingConstants.FLAG_COOLDOWN);
                throw new BusinessException(HttpStatus.BAD_REQUEST, EC_FLAG_COOLDOWN,
                        "提交过于频繁，请 " + Math.max(seconds, 1) + " 秒后重试该题");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[Volur-Training] 单题冷却校验异常: userId={}, challengeId={}", userId, sourceChallengeId, ex);
        }
    }

    private void checkGlobalSubmit(Long userId) {
        try {
            String key = TrainingConstants.KEY_FLAG_SUBMIT + userId;
            // SET NX EX 原子执行：避免 INCR 后进程崩溃遗留无 TTL 的孤儿 key 导致永久限流
            Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", TrainingConstants.FLAG_GLOBAL_WINDOW);
            if (!Boolean.TRUE.equals(acquired)) {
                long seconds = remainingSeconds(key, TrainingConstants.FLAG_GLOBAL_WINDOW);
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, EC_FLAG_GLOBAL_RATE_LIMITED,
                        "提交过于频繁，请 " + Math.max(seconds, 1) + " 秒后重试");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[Volur-Training] 全局提交限速校验异常: userId={}", userId, ex);
        }
    }

    private long remainingSeconds(String key, Duration fallback) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return fallback.toSeconds();
        }
        return ttl;
    }

    private static String formatSeconds(long seconds) {
        if (seconds >= 60) {
            return (seconds + 59) / 60 + " 分钟";
        }
        return Math.max(seconds, 1) + " 秒";
    }

}
