package com.humolix.volur.verification;

import com.humolix.volur.verification.captcha.manager.CaptchaProviderManager;
import com.humolix.volur.verification.captcha.provider.CaptchaProvider;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import com.humolix.volur.verification.risk.limiter.RedisSlidingWindowRateLimiter;
import com.humolix.volur.verification.service.impl.CaptchaServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class CaptchaRiskFlowTest {

    private CaptchaProviderManager providerManager;
    private RiskEvaluationEngine riskEngine;
    private RedisSlidingWindowRateLimiter rateLimiter;
    private StringRedisTemplate redisTemplate;
    private CaptchaServiceImpl captchaService;

    @BeforeEach
    public void setUp() {
        providerManager = Mockito.mock(CaptchaProviderManager.class);
        rateLimiter = Mockito.mock(RedisSlidingWindowRateLimiter.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        riskEngine = new RiskEvaluationEngine(rateLimiter, redisTemplate, null, null);
        captchaService = new CaptchaServiceImpl(providerManager, riskEngine, rateLimiter, redisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("123.123.123.123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // 默认放行限频
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
    }

    @Test
    public void testHighRiskWithoutTicketRequiresBehaviorVerify() {
        // 模拟 10 分钟内有 12 次失败记录，评级为 HIGH
        when(rateLimiter.getCurrentCount(contains("failures:"), any(Duration.class))).thenReturn(12L);
        when(rateLimiter.getCurrentCount(contains("velocity:"), any(Duration.class))).thenReturn(2L);

        CaptchaResponseVO response = captchaService.generateCaptcha(null, null);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getRequireBehaviorVerify(), "高风控下必须要求行为式验证");
        Assertions.assertNull(response.getImageBase64(), "高风控下严禁下发图形/滑块图片到普通显示框");
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE.name(), response.getCaptchaType());
    }

    @Test
    public void testHighRiskWithValidTicketReducesRiskAndReturnsGraphicCaptcha() {
        // 模拟初始高风险
        when(rateLimiter.getCurrentCount(contains("failures:"), any(Duration.class))).thenReturn(10L);
        when(rateLimiter.getCurrentCount(contains("velocity:"), any(Duration.class))).thenReturn(0L);

        // 模拟有效的 Ticket payload 在 Redis 中存在
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // payload: scene#verifiedType#ip#timestamp
        when(valueOps.get(contains("ticket:"))).thenReturn("LOGIN#SLIDE_PUZZLE#123.123.123.123#" + System.currentTimeMillis());

        // 模拟图形验证码 Provider
        CaptchaProvider lineProvider = Mockito.mock(CaptchaProvider.class);
        when(providerManager.getProvider(CaptchaType.LINE)).thenReturn(lineProvider);
        when(lineProvider.generate(eq(CaptchaScene.LOGIN), any(RiskLevel.class)))
                .thenReturn(CaptchaResponseVO.builder()
                        .captchaId("line-101")
                        .captchaType(CaptchaType.LINE.name())
                        .imageBase64("data:image/png;base64,mockGraphicContent")
                        .build());

        CaptchaResponseVO response = captchaService.generateCaptcha(null, "valid_ticket_uuid");

        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.getRequireBehaviorVerify(), "持有有效凭证核验通过后无需再次弹出人机验证");
        Assertions.assertEquals("data:image/png;base64,mockGraphicContent", response.getImageBase64(), "必须成功获得真正的图形验证码");

        // 验证调用了 reduceCount 扣减 6 次失败计数惩罚，而非直接全量重置清空
        Mockito.verify(rateLimiter).reduceCount(contains("failures:"), eq(6L));
    }

}
