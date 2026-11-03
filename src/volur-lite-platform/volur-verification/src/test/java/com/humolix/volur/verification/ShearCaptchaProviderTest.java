package com.humolix.volur.verification;

import com.humolix.volur.verification.captcha.provider.ShearCaptchaProvider;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShearCaptchaProviderTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ShearCaptchaProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        provider = new ShearCaptchaProvider(redisTemplate);
    }

    @Test
    void getType_returnsShear() {
        Assertions.assertEquals(CaptchaType.SHEAR, provider.getType());
    }

    @Test
    void generate_createsValidShearCaptcha() {
        CaptchaResponseVO response = provider.generate(CaptchaScene.LOGIN, RiskLevel.MEDIUM);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getCaptchaId());
        Assertions.assertEquals(CaptchaType.SHEAR.name(), response.getCaptchaType());
        Assertions.assertTrue(response.getImageBase64().startsWith("data:image/png;base64,"));
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void verify_matchedCode_returnsTrue() {
        String captchaId = "test-shear-id";
        String redisKey = "vredis:verification:captcha:shear:LOGIN:" + captchaId;
        when(valueOperations.get(eq(redisKey))).thenReturn("AB12CD");

        boolean result = provider.verify(captchaId, "ab12cd", CaptchaScene.LOGIN);

        Assertions.assertTrue(result);
        verify(redisTemplate).delete(eq(redisKey));
    }

    @Test
    void verify_mismatchedCode_returnsFalse() {
        String captchaId = "test-shear-id";
        String redisKey = "vredis:verification:captcha:shear:LOGIN:" + captchaId;
        when(valueOperations.get(eq(redisKey))).thenReturn("AB12CD");

        boolean result = provider.verify(captchaId, "WRONG1", CaptchaScene.LOGIN);

        Assertions.assertFalse(result);
    }

}
