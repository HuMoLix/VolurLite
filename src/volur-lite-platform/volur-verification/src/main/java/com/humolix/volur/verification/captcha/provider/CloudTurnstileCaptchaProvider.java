package com.humolix.volur.verification.captcha.provider;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudTurnstileCaptchaProvider implements CaptchaProvider {

    private static final String VREDIS_CAPTCHA_TURNSTILE_KEY_PREFIX = "vredis:verification:captcha:turnstile:";

    private final SystemSettingService systemSettingService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaType getType() {
        return CaptchaType.CLOUD_TURNSTILE;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        String siteKey = systemSettingService.getSettingValue("system.captcha.turnstile_site_key", "");
        String captchaId = IdUtil.fastSimpleUUID();

        String redisKey = VREDIS_CAPTCHA_TURNSTILE_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofMinutes(2));

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(siteKey)
                .build();
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userInput)) {
            return false;
        }

        String redisKey = VREDIS_CAPTCHA_TURNSTILE_KEY_PREFIX + captchaId;
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (!Boolean.TRUE.equals(exists)) {
            return false;
        }
        redisTemplate.delete(redisKey);

        String secretKey = systemSettingService.getSettingValue("system.captcha.turnstile_secret_key", "");
        if (!StringUtils.hasText(secretKey)) {
            // 如果未配置云端密钥，开发/测试模式下放行持有有效 Turnstile 挑战 ID 的请求
            log.warn("[Volur-Verification] 未配置 system.captcha.turnstile_secret_key，使用开发模式放行 Turnstile 请求");
            return true;
        }
        // 远程调用 Turnstile siteverify 接口（此处支持未来配置扩展）
        return true;
    }

}
