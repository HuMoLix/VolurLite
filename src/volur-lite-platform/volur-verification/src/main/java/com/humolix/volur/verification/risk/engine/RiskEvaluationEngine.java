package com.humolix.volur.verification.risk.engine;

import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEvaluationEngine {

    private static final String VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX = "vredis:verification:risk:failures:";
    private static final String VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX = "vredis:verification:risk:velocity:";

    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;

    public RiskLevel evaluateRisk(String ip, CaptchaScene scene) {
        if (ip == null || ip.isBlank()) {
            return RiskLevel.LOW;
        }

        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        long recentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));

        String velocityKey = VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX + ip;
        long recentVelocity = rateLimiter.getCurrentCount(velocityKey, Duration.ofMinutes(1));

        if (recentFailures >= 10) {
            log.warn("[Volur-Verification] IP [{}] 触发高风险评估 (10分钟内失败 {} 次)", ip, recentFailures);
            return RiskLevel.HIGH;
        }

        if (recentFailures >= 5 || recentVelocity >= 12) {
            log.info("[Volur-Verification] IP [{}] 触发中风险评估 (失败 {} 次, 1分钟速率 {} 次)", ip, recentFailures, recentVelocity);
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    public CaptchaType resolveEscalatedType(CaptchaType requestedType, RiskLevel riskLevel) {
        if (riskLevel == RiskLevel.HIGH) {
            return CaptchaType.SLIDE_PUZZLE;
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            return requestedType == CaptchaType.SLIDE_PUZZLE ? CaptchaType.SLIDE_PUZZLE : CaptchaType.SHEAR;
        }
        return requestedType != null ? requestedType : CaptchaType.LINE;
    }

    public void recordFailure(String ip, CaptchaScene scene) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        rateLimiter.tryAcquire(failureKey, 100, Duration.ofMinutes(10));
    }

    public void recordVelocity(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String velocityKey = VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX + ip;
        rateLimiter.tryAcquire(velocityKey, 100, Duration.ofMinutes(1));
    }

    public void recordSuccess(String ip, CaptchaScene scene) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        try {
            redisTemplate.delete(failureKey);
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 清理成功 IP 风控记录警告: ip={}", ip, ex);
        }
    }

}
