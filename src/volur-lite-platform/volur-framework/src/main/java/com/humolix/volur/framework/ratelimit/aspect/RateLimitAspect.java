package com.humolix.volur.framework.ratelimit.aspect;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.framework.ratelimit.annotation.RateLimit;
import com.humolix.volur.framework.ratelimit.annotation.RateLimits;
import com.humolix.volur.framework.ratelimit.enums.RateLimitType;
import com.humolix.volur.framework.ratelimit.service.RateLimitService;
import com.humolix.volur.framework.security.CurrentUserSchema;
import com.humolix.volur.framework.security.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.humolix.volur.framework.ratelimit.annotation.RateLimit) || @annotation(com.humolix.volur.framework.ratelimit.annotation.RateLimits)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit[] annotations = method.getAnnotationsByType(RateLimit.class);
        if (annotations == null || annotations.length == 0) {
            return joinPoint.proceed();
        }

        EvaluationContext context = buildEvaluationContext(joinPoint, method);
        List<ParsedRateLimitRule> rules = new ArrayList<>(annotations.length);

        for (RateLimit annotation : annotations) {
            String resolvedKey = resolveKey(annotation.key(), context, joinPoint);
            String module = resolveModule(annotation.module(), joinPoint);
            rules.add(new ParsedRateLimitRule(annotation, module, resolvedKey));
        }

        for (ParsedRateLimitRule rule : rules) {
            checkRateLimitPreProceed(rule);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            handleFailure(rules, t);
            throw t;
        }

        handleSuccess(rules);
        return result;
    }

    private void checkRateLimitPreProceed(ParsedRateLimitRule rule) {
        RateLimit annotation = rule.annotation();
        RateLimitType type = annotation.type();
        String module = rule.module();
        String key = rule.resolvedKey();

        if (type == RateLimitType.BRUTE_FORCE) {
            String lockKey = RateLimitService.VREDIS_RATELIMIT_BRUTE_LOCK_KEY_PREFIX + module + ":" + key;
            if (rateLimitService.isBruteForceLocked(lockKey)) {
                long remaining = rateLimitService.getBruteForceLockRemainingSeconds(lockKey);
                String message = formatLockMessage(annotation.message(), remaining);
                int code = annotation.errorCode() != 0 ? annotation.errorCode() : annotation.status().value();
                throw new BusinessException(annotation.status(), code, message);
            }
            return;
        }

        if (type == RateLimitType.SLIDING_WINDOW) {
            String slidingKey = RateLimitService.VREDIS_RATELIMIT_SLIDING_KEY_PREFIX + module + ":" + key;
            boolean acquired = rateLimitService.tryAcquireSliding(
                    slidingKey,
                    annotation.count(),
                    Duration.ofSeconds(annotation.window())
            );
            if (!acquired) {
                int code = annotation.errorCode() != 0 ? annotation.errorCode() : annotation.status().value();
                throw new BusinessException(annotation.status(), code, annotation.message());
            }
            return;
        }

        if (type == RateLimitType.FIXED_WINDOW) {
            String fixedKey = RateLimitService.VREDIS_RATELIMIT_FIXED_KEY_PREFIX + module + ":" + key;
            boolean acquired = rateLimitService.tryAcquireFixed(
                    fixedKey,
                    annotation.count(),
                    Duration.ofSeconds(annotation.window())
            );
            if (!acquired) {
                int code = annotation.errorCode() != 0 ? annotation.errorCode() : annotation.status().value();
                throw new BusinessException(annotation.status(), code, annotation.message());
            }
        }
    }

    private void handleSuccess(List<ParsedRateLimitRule> rules) {
        for (ParsedRateLimitRule rule : rules) {
            if (rule.annotation().type() == RateLimitType.BRUTE_FORCE) {
                String failKey = RateLimitService.VREDIS_RATELIMIT_BRUTE_FAIL_KEY_PREFIX + rule.module() + ":" + rule.resolvedKey();
                rateLimitService.recordBruteForceSuccess(failKey);
            }
        }
    }

    private void handleFailure(List<ParsedRateLimitRule> rules, Throwable t) {
        if (t instanceof BusinessException be && be.getStatus().value() == 429) {
            return;
        }

        for (ParsedRateLimitRule rule : rules) {
            if (rule.annotation().type() == RateLimitType.BRUTE_FORCE) {
                RateLimit annotation = rule.annotation();
                String failKey = RateLimitService.VREDIS_RATELIMIT_BRUTE_FAIL_KEY_PREFIX + rule.module() + ":" + rule.resolvedKey();
                String lockKey = RateLimitService.VREDIS_RATELIMIT_BRUTE_LOCK_KEY_PREFIX + rule.module() + ":" + rule.resolvedKey();
                boolean locked = rateLimitService.recordBruteForceFailure(
                        failKey,
                        lockKey,
                        annotation.count(),
                        Duration.ofSeconds(annotation.window()),
                        Duration.ofSeconds(annotation.lockSeconds())
                );
                if (locked) {
                    long remaining = rateLimitService.getBruteForceLockRemainingSeconds(lockKey);
                    String message = formatLockMessage(annotation.message(), remaining);
                    int code = annotation.errorCode() != 0 ? annotation.errorCode() : annotation.status().value();
                    throw new BusinessException(annotation.status(), code, message);
                }
            }
        }
    }

    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        String remoteIp = RequestUtil.getRemoteIp();
        String originalUri = RequestUtil.getOriginalRequestUri();
        context.setVariable("ip", remoteIp);
        context.setVariable("uri", originalUri);

        try {
            CurrentUserSchema userSchema = SecurityContextUtil.getCurrentUserSchema();
            if (userSchema != null) {
                context.setVariable("userId", userSchema.getId());
                context.setVariable("username", userSchema.getUsername());
            } else {
                context.setVariable("userId", 0L);
                context.setVariable("username", "anonymous");
            }
        } catch (Exception ex) {
            context.setVariable("userId", 0L);
            context.setVariable("username", "anonymous");
        }

        return context;
    }

    private String resolveKey(String rawKey, EvaluationContext context, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(rawKey)) {
            return RequestUtil.getRemoteIp();
        }
        if (!rawKey.contains("#") && !rawKey.contains("T(") && !rawKey.contains("+")) {
            return rawKey.trim();
        }
        try {
            Object value = expressionParser.parseExpression(rawKey).getValue(context);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString().trim().toLowerCase();
            }
        } catch (Exception ex) {
            log.warn("[RateLimit] SpEL key parse failed for expression: {}, fallback to remoteIp", rawKey, ex);
        }
        return RequestUtil.getRemoteIp();
    }

    private String resolveModule(String configuredModule, ProceedingJoinPoint joinPoint) {
        if (StringUtils.hasText(configuredModule)) {
            return configuredModule.trim().toLowerCase();
        }
        return joinPoint.getTarget().getClass().getSimpleName().toLowerCase();
    }

    private String formatLockMessage(String templateMessage, long remainingSeconds) {
        if (!StringUtils.hasText(templateMessage)) {
            templateMessage = "尝试失败次数过多，已被锁定";
        }
        String timeStr = remainingSeconds >= 60
                ? ((remainingSeconds + 59) / 60) + "分钟"
                : Math.max(remainingSeconds, 1) + "秒";
        if (templateMessage.contains("{}")) {
            return templateMessage.replace("{}", timeStr);
        }
        if (templateMessage.contains("%s")) {
            return String.format(templateMessage, timeStr);
        }
        return templateMessage + "，请约 " + timeStr + " 后再试";
    }

    private record ParsedRateLimitRule(RateLimit annotation, String module, String resolvedKey) {
    }
}
