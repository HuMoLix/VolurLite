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
        if (ip == null || ip.isBlank() || isBypassedIp(ip)) {
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

    public CaptchaType resolveEscalatedType(CaptchaType requestedType, RiskLevel riskLevel, CaptchaScene scene) {
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        CaptchaType minType = effectiveScene.getMinRequiredType();

        CaptchaType baseType;
        if (requestedType == null || !requestedType.isAtLeast(minType)) {
            baseType = minType;
        } else {
            baseType = requestedType;
        }

        if (riskLevel == RiskLevel.HIGH) {
            return baseType.isAtLeast(CaptchaType.SLIDE_PUZZLE) ? baseType : CaptchaType.SLIDE_PUZZLE;
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            if (baseType == CaptchaType.LINE) {
                return CaptchaType.SHEAR;
            }
            return baseType;
        }

        return baseType;
    }

    public void recordFailure(String ip, CaptchaScene scene) {
        if (ip == null || ip.isBlank() || isBypassedIp(ip)) {
            return;
        }
        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        rateLimiter.tryAcquire(failureKey, 100, Duration.ofMinutes(10));
    }

    public void recordVelocity(String ip) {
        if (ip == null || ip.isBlank() || isBypassedIp(ip)) {
            return;
        }
        String velocityKey = VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX + ip;
        rateLimiter.tryAcquire(velocityKey, 100, Duration.ofMinutes(1));
    }

    public void reduceRisk(String ip) {
        if (ip == null || ip.isBlank() || isBypassedIp(ip)) {
            return;
        }
        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        // 降低 6 次失败计数惩罚，使其脱离高危阈值，但保留部分历史风险底数，若后续持续异常则迅速再次升险
        rateLimiter.reduceCount(failureKey, 6);
    }

    public void recordSuccess(String ip, CaptchaScene scene) {
        reduceRisk(ip);
    }

    private boolean isBypassedIp(String ip) {
        String clean = ip.trim();
        return "127.0.0.1".equals(clean) || "0:0:0:0:0:0:0:1".equals(clean) || "localhost".equalsIgnoreCase(clean);
    }

}
