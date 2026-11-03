package com.humolix.volur.verification.risk.engine;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.event.RiskAutoBanEvent;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEvaluationEngine {

    private static final String VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX = "vredis:verification:risk:failures:";
    private static final String VREDIS_VERIFICATION_RISK_ACCOUNT_FAILURES_KEY_PREFIX = "vredis:verification:risk:account_failures:";
    private static final String VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX = "vredis:verification:risk:velocity:";
    private static final String VREDIS_VERIFICATION_RISK_DEVICE_FAILURES_KEY_PREFIX = "vredis:verification:risk:device_failures:";
    private static final String VREDIS_VERIFICATION_RISK_DEVICE_VELOCITY_KEY_PREFIX = "vredis:verification:risk:device_velocity:";
    public static final String VREDIS_VERIFICATION_RISK_CLIENT_PROFILE_KEY_PREFIX = "vredis:verification:risk:client_profile:";

    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final SystemSettingService systemSettingService;
    private final ApplicationEventPublisher eventPublisher;

    public record ParsedClientProfile(
            String deviceId,
            String deviceHash,
            String canvasHash,
            String browserFamily,
            boolean isHeadless,
            long timestamp
    ) {}

    public RiskLevel evaluateRisk(String ip, CaptchaScene scene) {
        if (ip == null || ip.isBlank() || isBypassedIp(ip)) {
            return RiskLevel.LOW;
        }

        if (!isRiskEnabled()) {
            return RiskLevel.LOW;
        }

        String failureKey = VREDIS_VERIFICATION_RISK_FAILURES_KEY_PREFIX + ip;
        long recentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));

        String velocityKey = VREDIS_VERIFICATION_RISK_VELOCITY_KEY_PREFIX + ip;
        long recentVelocity = rateLimiter.getCurrentCount(velocityKey, Duration.ofMinutes(1));

        int highFailures = getIntSetting(SystemSettingKeys.RISK_FAILURES_HIGH_THRESHOLD, 10);
        int mediumFailures = getIntSetting(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD, 5);
        int mediumVelocity = getIntSetting(SystemSettingKeys.RISK_VELOCITY_MEDIUM_THRESHOLD, 12);

        if (recentFailures >= highFailures) {
            log.warn("[Volur-Verification] IP [{}] 触发高风险评估 (10分钟内失败 {} 次，阈值 {})", ip, recentFailures, highFailures);
            return RiskLevel.HIGH;
        }

        if (recentFailures >= mediumFailures || recentVelocity >= mediumVelocity) {
            log.info("[Volur-Verification] IP [{}] 触发中风险评估 (失败 {} 次, 1分钟速率 {} 次)", ip, recentFailures, recentVelocity);
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    public RiskLevel evaluateComprehensiveRisk(String ip, String account, CaptchaScene scene) {
        return evaluateComprehensiveRisk(ip, account, null, scene);
    }

    public RiskLevel evaluateComprehensiveRisk(String ip, String account, String explicitDeviceToken, CaptchaScene scene) {
        RiskLevel ipRisk = evaluateRisk(ip, scene);
        RiskLevel accountRisk = evaluateAccountRisk(account, scene);

        String deviceToken = StringUtils.hasText(explicitDeviceToken)
                ? explicitDeviceToken
                : RequestUtil.getDeviceFingerprint();

        RiskLevel deviceRisk = evaluateDeviceRisk(deviceToken, scene);

        String userAgent = RequestUtil.getUserAgent();
        String tlsProtocol = RequestUtil.getTlsProtocol();
        String tlsCipher = RequestUtil.getTlsCipher();
        String tlsAlpn = RequestUtil.getTlsAlpn();

        RiskLevel tlsRisk = evaluateTlsAndBrowserConsistency(userAgent, tlsProtocol, tlsCipher, tlsAlpn);

        // 异步缓存客户端画像供 IP 诊断探针实时检索
        cacheClientProfile(ip, deviceToken, userAgent, tlsProtocol, tlsCipher, tlsAlpn);

        RiskLevel maxRisk = ipRisk;
        if (accountRisk.ordinal() > maxRisk.ordinal()) {
            maxRisk = accountRisk;
        }
        if (deviceRisk.ordinal() > maxRisk.ordinal()) {
            maxRisk = deviceRisk;
        }
        if (tlsRisk.ordinal() > maxRisk.ordinal()) {
            maxRisk = tlsRisk;
        }
        return maxRisk;
    }

    public RiskLevel evaluateAccountRisk(String account, CaptchaScene scene) {
        if (account == null || account.isBlank()) {
            return RiskLevel.LOW;
        }
        if (!isRiskEnabled()) {
            return RiskLevel.LOW;
        }
        String cleanAccount = account.trim().toLowerCase();
        String failureKey = VREDIS_VERIFICATION_RISK_ACCOUNT_FAILURES_KEY_PREFIX + cleanAccount;
        long recentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));

        int accountHigh = getIntSetting(SystemSettingKeys.RISK_ACCOUNT_FAILURES_HIGH_THRESHOLD, 8);
        int accountMedium = getIntSetting(SystemSettingKeys.RISK_ACCOUNT_FAILURES_MEDIUM_THRESHOLD, 4);

        if (recentFailures >= accountHigh) {
            log.warn("[Volur-Verification] 账号 [{}] 触发高风险评估 (10分钟内失败 {} 次，阈值 {})", cleanAccount, recentFailures, accountHigh);
            return RiskLevel.HIGH;
        }

        if (recentFailures >= accountMedium) {
            log.info("[Volur-Verification] 账号 [{}] 触发中风险评估 (10分钟内失败 {} 次，阈值 {})", cleanAccount, recentFailures, accountMedium);
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    /**
     * 评估设备与 Canvas 指纹环境安全风险
     */
    public RiskLevel evaluateDeviceRisk(String rawToken, CaptchaScene scene) {
        if (!StringUtils.hasText(rawToken)) {
            return RiskLevel.LOW;
        }
        if (!isRiskEnabled() || !isFingerprintEnabled()) {
            return RiskLevel.LOW;
        }

        ParsedClientProfile profile = parseDeviceToken(rawToken);
        if (profile == null) {
            return RiskLevel.LOW;
        }

        // 1. 无头浏览器严控惩罚 (若标记为无头浏览器且开启惩罚)
        if (profile.isHeadless() && isHeadlessPenaltyEnabled()) {
            log.warn("[Volur-Verification] 检测到无头浏览器或自动化模拟器特征: deviceHash={}, browserFamily={}",
                    profile.deviceHash(), profile.browserFamily());
            return RiskLevel.HIGH;
        }

        String deviceHash = profile.deviceHash();
        if (!StringUtils.hasText(deviceHash)) {
            return RiskLevel.LOW;
        }

        // 2. 跨 IP 设备滑动窗口失败惩罚
        String failureKey = VREDIS_VERIFICATION_RISK_DEVICE_FAILURES_KEY_PREFIX + deviceHash;
        long recentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));

        String velocityKey = VREDIS_VERIFICATION_RISK_DEVICE_VELOCITY_KEY_PREFIX + deviceHash;
        long recentVelocity = rateLimiter.getCurrentCount(velocityKey, Duration.ofMinutes(1));

        int highFailures = getIntSetting(SystemSettingKeys.RISK_FAILURES_HIGH_THRESHOLD, 10);
        int mediumFailures = getIntSetting(SystemSettingKeys.RISK_FAILURES_MEDIUM_THRESHOLD, 5);
        int mediumVelocity = getIntSetting(SystemSettingKeys.RISK_VELOCITY_MEDIUM_THRESHOLD, 12);

        if (recentFailures >= highFailures) {
            log.warn("[Volur-Verification] 设备 [{}] 触发高风险评估 (10分钟内失败 {} 次)", deviceHash, recentFailures);
            return RiskLevel.HIGH;
        }
        if (recentFailures >= mediumFailures || recentVelocity >= mediumVelocity) {
            log.info("[Volur-Verification] 设备 [{}] 触发中风险评估 (失败 {} 次, 1分钟速率 {} 次)",
                    deviceHash, recentFailures, recentVelocity);
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    /**
     * 评估原生 Nginx TLS 握手特征与客户端浏览器声明的一致性
     * 重点：白名单保护 Safari, Edge, Firefox, Chrome 正常访问；严厉拦截伪造 UA 的纯脚本。
     */
    public RiskLevel evaluateTlsAndBrowserConsistency(String userAgent, String tlsProtocol, String tlsCipher, String tlsAlpn) {
        if (!isRiskEnabled() || !isTlsCheckEnabled()) {
            return RiskLevel.LOW;
        }
        // 若无 TLS 协议头（纯 HTTP 模式、本地开发环境或上游代理未开启 SSL），优雅降级放行
        if (!StringUtils.hasText(tlsProtocol)) {
            return RiskLevel.LOW;
        }

        if (!StringUtils.hasText(userAgent)) {
            return RiskLevel.MEDIUM;
        }

        String lowerUa = userAgent.toLowerCase();

        // 1. 明确的纯自动化发包工具 User-Agent 嗅探
        if (lowerUa.contains("python-requests") || lowerUa.contains("aiohttp")
                || lowerUa.contains("urllib") || lowerUa.contains("go-http-client")
                || lowerUa.contains("curl/") || lowerUa.contains("postmanruntime")) {
            log.warn("[Volur-Verification] 检测到自动化脚本 User-Agent: ua={}, tlsProto={}, alpn={}",
                    userAgent, tlsProtocol, tlsAlpn);
            return RiskLevel.HIGH;
        }

        // 2. 主流浏览器家族白名单识别 (Safari / Edge / Firefox / Chrome)
        boolean isSafari = lowerUa.contains("safari") && !lowerUa.contains("chrome") && !lowerUa.contains("crios") && !lowerUa.contains("android");
        boolean isEdge = lowerUa.contains("edg/");
        boolean isFirefox = lowerUa.contains("firefox") || lowerUa.contains("fxios");
        boolean isChrome = (lowerUa.contains("chrome") || lowerUa.contains("crios")) && !isEdge;

        if (isSafari || isEdge || isFirefox || isChrome) {
            // 合法主流浏览器均支持 TLS 1.2 或 TLS 1.3
            // 只要 TLS 协商正常且没有伪造特征，判定为 LOW 正常通行
            return RiskLevel.LOW;
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

        long currentFailures = rateLimiter.getCurrentCount(failureKey, Duration.ofMinutes(10));
        int banThreshold = getIntSetting(SystemSettingKeys.RISK_FAILURES_BAN_THRESHOLD, 20);
        if (banThreshold > 0 && currentFailures >= banThreshold && eventPublisher != null) {
            eventPublisher.publishEvent(new RiskAutoBanEvent(this, ip, "连续10分钟内验证失败 " + currentFailures + " 次触发自动封禁"));
        }
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
        int step = getIntSetting(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, 6);
        rateLimiter.reduceCount(failureKey, step);
    }

    public void recordSuccess(String ip, CaptchaScene scene) {
        reduceRisk(ip);
    }

    public void recordAccountFailure(String account, CaptchaScene scene) {
        if (account == null || account.isBlank()) {
            return;
        }
        String cleanAccount = account.trim().toLowerCase();
        String failureKey = VREDIS_VERIFICATION_RISK_ACCOUNT_FAILURES_KEY_PREFIX + cleanAccount;
        rateLimiter.tryAcquire(failureKey, 100, Duration.ofMinutes(10));
    }

    public void reduceAccountRisk(String account) {
        if (account == null || account.isBlank()) {
            return;
        }
        String cleanAccount = account.trim().toLowerCase();
        String failureKey = VREDIS_VERIFICATION_RISK_ACCOUNT_FAILURES_KEY_PREFIX + cleanAccount;
        int step = getIntSetting(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, 6);
        rateLimiter.reduceCount(failureKey, step);
    }

    public void recordAccountSuccess(String account, CaptchaScene scene) {
        reduceAccountRisk(account);
    }

    public void recordDeviceFailure(String rawToken, CaptchaScene scene) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }
        ParsedClientProfile profile = parseDeviceToken(rawToken);
        String deviceHash = profile != null ? profile.deviceHash() : rawToken.trim();
        if (StringUtils.hasText(deviceHash)) {
            String failureKey = VREDIS_VERIFICATION_RISK_DEVICE_FAILURES_KEY_PREFIX + deviceHash;
            rateLimiter.tryAcquire(failureKey, 100, Duration.ofMinutes(10));
        }
    }

    public void recordDeviceVelocity(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }
        ParsedClientProfile profile = parseDeviceToken(rawToken);
        String deviceHash = profile != null ? profile.deviceHash() : rawToken.trim();
        if (StringUtils.hasText(deviceHash)) {
            String velocityKey = VREDIS_VERIFICATION_RISK_DEVICE_VELOCITY_KEY_PREFIX + deviceHash;
            rateLimiter.tryAcquire(velocityKey, 100, Duration.ofMinutes(1));
        }
    }

    public void reduceDeviceRisk(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }
        ParsedClientProfile profile = parseDeviceToken(rawToken);
        String deviceHash = profile != null ? profile.deviceHash() : rawToken.trim();
        if (StringUtils.hasText(deviceHash)) {
            String failureKey = VREDIS_VERIFICATION_RISK_DEVICE_FAILURES_KEY_PREFIX + deviceHash;
            int step = getIntSetting(SystemSettingKeys.RISK_RECOVERY_STEP_COUNT, 6);
            rateLimiter.reduceCount(failureKey, step);
        }
    }

    public ParsedClientProfile parseDeviceToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        String clean = rawToken.trim();
        try {
            String jsonStr = clean;
            if (!clean.startsWith("{")) {
                byte[] decoded = Base64.getDecoder().decode(clean);
                jsonStr = new String(decoded, StandardCharsets.UTF_8);
            }
            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                JSONObject obj = JSONUtil.parseObj(jsonStr);
                String did = obj.getStr("did", "");
                String dh = obj.getStr("dh", "");
                String ch = obj.getStr("ch", "");
                String bf = obj.getStr("bf", "OTHER");
                boolean hl = obj.getInt("hl", 0) == 1;
                long ts = obj.getLong("ts", 0L);
                return new ParsedClientProfile(did, dh, ch, bf, hl, ts);
            }
        } catch (Exception ignored) {
        }
        return new ParsedClientProfile("", clean, "", "OTHER", false, System.currentTimeMillis());
    }

    public void cacheClientProfile(String ip, String deviceToken, String userAgent, String tlsProtocol, String tlsCipher, String tlsAlpn) {
        if (ip == null || ip.isBlank() || redisTemplate == null) {
            return;
        }
        try {
            ParsedClientProfile profile = parseDeviceToken(deviceToken);
            String devHash = profile != null ? profile.deviceHash() : (deviceToken != null ? deviceToken : "-");
            String bf = profile != null ? profile.browserFamily() : detectBrowserFamilyFromUa(userAgent);
            String proto = StringUtils.hasText(tlsProtocol) ? tlsProtocol : "-";
            String cipher = StringUtils.hasText(tlsCipher) ? tlsCipher : "-";
            String alpn = StringUtils.hasText(tlsAlpn) ? tlsAlpn : "-";

            String val = devHash + ":::" + bf + ":::" + proto + ":::" + cipher + ":::" + alpn;
            String key = VREDIS_VERIFICATION_RISK_CLIENT_PROFILE_KEY_PREFIX + ip.trim();
            redisTemplate.opsForValue().set(key, val, Duration.ofHours(24));
        } catch (Exception e) {
            log.debug("[Volur-Verification] 缓存客户端画像失败: {}", e.getMessage());
        }
    }

    private String detectBrowserFamilyFromUa(String ua) {
        if (!StringUtils.hasText(ua)) {
            return "OTHER";
        }
        String lower = ua.toLowerCase();
        if (lower.contains("edg/")) {
            return "EDGE";
        }
        if (lower.contains("firefox") || lower.contains("fxios")) {
            return "FIREFOX";
        }
        if (lower.contains("safari") && !lower.contains("chrome") && !lower.contains("crios") && !lower.contains("android")) {
            return "SAFARI";
        }
        if (lower.contains("chrome") || lower.contains("crios") || lower.contains("chromium")) {
            return "CHROME";
        }
        return "OTHER";
    }

    private boolean isRiskEnabled() {
        return systemSettingService == null || systemSettingService.getBoolean(SystemSettingKeys.RISK_ENABLED, true);
    }

    private boolean isFingerprintEnabled() {
        return systemSettingService == null || systemSettingService.getBoolean(SystemSettingKeys.RISK_FINGERPRINT_ENABLED, true);
    }

    private boolean isHeadlessPenaltyEnabled() {
        return systemSettingService == null || systemSettingService.getBoolean(SystemSettingKeys.RISK_HEADLESS_PENALTY_ENABLED, true);
    }

    private boolean isTlsCheckEnabled() {
        return systemSettingService == null || systemSettingService.getBoolean(SystemSettingKeys.RISK_TLS_CHECK_ENABLED, true);
    }

    private int getIntSetting(String key, int defaultValue) {
        return systemSettingService != null ? systemSettingService.getInt(key, defaultValue) : defaultValue;
    }

    private boolean isBypassedIp(String ip) {
        String clean = ip.trim();
        return "127.0.0.1".equals(clean) || "0:0:0:0:0:0:0:1".equals(clean) || "localhost".equalsIgnoreCase(clean);
    }

}
