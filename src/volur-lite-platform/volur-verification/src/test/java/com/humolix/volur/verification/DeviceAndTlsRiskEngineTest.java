package com.humolix.volur.verification;

import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

public class DeviceAndTlsRiskEngineTest {

    private RiskEvaluationEngine riskEngine;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        rateLimiter = Mockito.mock(RedisSlidingWindowRateLimiter.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        riskEngine = new RiskEvaluationEngine(rateLimiter, redisTemplate, null, null);
    }

    @Test
    public void testSafariBrowserTlsConsistencyAllowed() {
        String safariUa = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(safariUa, "TLSv1.3", "TLS_AES_128_GCM_SHA256", "h2");
        Assertions.assertEquals(RiskLevel.LOW, level, "Safari 用户正常访问必须放行");
    }

    @Test
    public void testEdgeBrowserTlsConsistencyAllowed() {
        String edgeUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(edgeUa, "TLSv1.3", "TLS_AES_128_GCM_SHA256", "h2");
        Assertions.assertEquals(RiskLevel.LOW, level, "Edge 用户正常访问必须放行");
    }

    @Test
    public void testFirefoxBrowserTlsConsistencyAllowed() {
        String firefoxUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(firefoxUa, "TLSv1.3", "TLS_AES_128_GCM_SHA256", "h2");
        Assertions.assertEquals(RiskLevel.LOW, level, "Firefox 用户正常访问必须放行");
    }

    @Test
    public void testChromeBrowserTlsConsistencyAllowed() {
        String chromeUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(chromeUa, "TLSv1.3", "TLS_AES_128_GCM_SHA256", "h2");
        Assertions.assertEquals(RiskLevel.LOW, level, "Chrome 用户正常访问必须放行");
    }

    @Test
    public void testPythonRequestsScriptBlocked() {
        String pythonUa = "python-requests/2.31.0";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(pythonUa, "TLSv1.3", "ECDHE-RSA-AES128-GCM-SHA256", "http/1.1");
        Assertions.assertEquals(RiskLevel.HIGH, level, "Python requests 发包脚本必须判定为高风险");
    }

    @Test
    public void testCurlScriptBlocked() {
        String curlUa = "curl/8.4.0";
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency(curlUa, "TLSv1.3", "TLS_AES_128_GCM_SHA256", "http/1.1");
        Assertions.assertEquals(RiskLevel.HIGH, level, "curl 命令行发包必须判定为高风险");
    }

    @Test
    public void testHeadlessBrowserPenalty() {
        String payloadJson = "{\"dh\":\"dev1234567890abc\",\"ch\":\"canv1234\",\"bf\":\"CHROME\",\"hl\":1,\"ts\":1727000000000}";
        String token = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        RiskLevel level = riskEngine.evaluateDeviceRisk(token, CaptchaScene.LOGIN);
        Assertions.assertEquals(RiskLevel.HIGH, level, "无头浏览器 (Headless/Puppeteer) 必须判定为高风险");
    }

    @Test
    public void testDeviceSlidingWindowFailuresEscalates() {
        String payloadJson = "{\"dh\":\"legit_device_01\",\"ch\":\"canv1234\",\"bf\":\"SAFARI\",\"hl\":0,\"ts\":1727000000000}";
        String token = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // 模拟该设备在 10 分钟内累积失败 11 次 (阈值 10 次)
        when(rateLimiter.getCurrentCount(contains("device_failures:"), any(Duration.class))).thenReturn(11L);

        RiskLevel level = riskEngine.evaluateDeviceRisk(token, CaptchaScene.LOGIN);
        Assertions.assertEquals(RiskLevel.HIGH, level, "跨 IP 相同设备连续失败超出阈值必须升为高风险");
    }

    @Test
    public void testPlainHttpFallbackPermitted() {
        // 当在本地开发或普通 HTTP 访问 (无 TLS 协议协商时)，不误杀任何客户端
        RiskLevel level = riskEngine.evaluateTlsAndBrowserConsistency("Mozilla/5.0", null, null, null);
        Assertions.assertEquals(RiskLevel.LOW, level, "无 TLS 协议头时优雅降级放行");
    }

}
