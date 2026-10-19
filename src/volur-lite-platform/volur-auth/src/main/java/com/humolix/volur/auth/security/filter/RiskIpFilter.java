package com.humolix.volur.auth.security.filter;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.admin.AdminRiskService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;

/**
 * 全局 IP 黑名单与动态滑动窗口频控防御过滤器
 * 在 JWT 认证过滤器执行前即时生效：
 * 1. 拦截命中黑名单的恶意来源 IP (403 Forbidden)
 * 2. 对非放行 IP 执行基于 Redis 的滑动窗口接口请求频控限流 (429 Too Many Requests)
 */
@Slf4j
@Component
public class RiskIpFilter extends OncePerRequestFilter {

    private static final String VREDIS_SECURITY_RATE_LIMIT_IP_PREFIX = "vredis:security:rate_limit:ip:";

    private final AdminRiskService adminRiskService;
    private final SystemSettingService systemSettingService;
    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final HandlerExceptionResolver resolver;

    public RiskIpFilter(
            AdminRiskService adminRiskService,
            SystemSettingService systemSettingService,
            RedisSlidingWindowRateLimiter rateLimiter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.adminRiskService = adminRiskService;
        this.systemSettingService = systemSettingService;
        this.rateLimiter = rateLimiter;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        if ("/error".equals(requestUri) || "/favicon.ico".equals(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = RequestUtil.getClientIp(request);

        // 1. IP 黑名单硬阻断校验 (403 Forbidden)
        if (adminRiskService.isIpBanned(clientIp)) {
            log.warn("[RiskIpFilter] 拦截到已被封禁的恶意 IP 请求: [{}] -> {}", clientIp, requestUri);
            String notice = systemSettingService.getSettingValue(
                    SystemSettingKeys.RISK_BAN_NOTICE_MESSAGE,
                    "您的 IP 存在异常访问行为，已被系统安全机制拦截。如需解封请联系平台管理员。"
            );
            resolver.resolveException(request, response, null, new BusinessException(HttpStatus.FORBIDDEN, notice));
            return;
        }

        // 2. 检查全局风控开关与本地回环放行，执行前置滑动窗口频控限流 (429 Too Many Requests)
        if (isRiskRateLimitEnabled() && !isBypassedIp(clientIp)) {
            int windowSeconds = systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, 60);
            int maxRequests = systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_MAX_REQUESTS, 120);

            String rateKey = VREDIS_SECURITY_RATE_LIMIT_IP_PREFIX + clientIp;
            boolean allowed = rateLimiter.tryAcquire(rateKey, maxRequests, Duration.ofSeconds(windowSeconds));

            if (!allowed) {
                log.warn("[RiskIpFilter] IP [{}] 触发全局安全滑动窗口频控限流: window={}s, maxRequests={}, uri={}",
                        clientIp, windowSeconds, maxRequests, requestUri);
                resolver.resolveException(request, response, null,
                        new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，已触发系统安全频控保护，请稍后再试"));
                return;
            }
        }

        // 3. 跨 IP 设备维度前置滑动窗口限流 (从 Cookie 提取设备指纹，抵御海量代理池脚本攻击)
        String deviceToken = RequestUtil.getDeviceFingerprint(request);
        if (isRiskRateLimitEnabled() && StringUtils.hasText(deviceToken)) {
            int windowSeconds = systemSettingService.getInt(SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW, 60);
            int maxDeviceRequests = systemSettingService.getInt(SystemSettingKeys.RISK_DEVICE_RATE_LIMIT_MAX, 100);
            String cleanDevice = deviceToken.trim();
            if (cleanDevice.length() > 32) {
                cleanDevice = cleanDevice.substring(0, 32);
            }
            String deviceRateKey = "vredis:security:rate_limit:device:" + cleanDevice;
            boolean deviceAllowed = rateLimiter.tryAcquire(deviceRateKey, maxDeviceRequests, Duration.ofSeconds(windowSeconds));
            if (!deviceAllowed) {
                log.warn("[RiskIpFilter] 设备 [{}] 跨 IP 触发设备滑动窗口频控限流: window={}s, maxRequests={}, ip={}, uri={}",
                        cleanDevice, windowSeconds, maxDeviceRequests, clientIp, requestUri);
                resolver.resolveException(request, response, null,
                        new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "该设备请求过于频繁，已触发系统安全频控保护，请稍后再试"));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRiskRateLimitEnabled() {
        return systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true);
    }

    private boolean isBypassedIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        boolean bypassLoopback = systemSettingService.getBoolean(SystemSettingKeys.RISK_BYPASS_LOOPBACK, true);
        if (bypassLoopback) {
            String clean = ip.trim();
            return "127.0.0.1".equals(clean) || "0:0:0:0:0:0:0:1".equals(clean)
                    || "::1".equals(clean) || "localhost".equalsIgnoreCase(clean);
        }
        return false;
    }

}
