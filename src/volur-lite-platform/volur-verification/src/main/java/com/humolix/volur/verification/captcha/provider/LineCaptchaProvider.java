package com.humolix.volur.verification.captcha.provider;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
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
public class LineCaptchaProvider implements CaptchaProvider {

    private static final String VREDIS_CAPTCHA_LINE_KEY_PREFIX = "vredis:verification:captcha:line:";
    private static final String BASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CAPTCHA_IMAGE_TYPE = "data:image/png;base64,";
    private static final int CAPTCHA_LENGTH = 6;
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;

    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaType getType() {
        return CaptchaType.LINE;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        RandomGenerator randomGenerator = new RandomGenerator(BASE_CHARS, CAPTCHA_LENGTH);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT);
        captcha.setGenerator(randomGenerator);
        captcha.createCode();

        String captchaId = IdUtil.fastSimpleUUID();
        String code = captcha.getCode();

        String redisKey = VREDIS_CAPTCHA_LINE_KEY_PREFIX + scene.name() + ":" + captchaId;
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(2));

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(CAPTCHA_IMAGE_TYPE + captcha.getImageBase64())
                .build();
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userInput)) {
            return false;
        }
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String redisKey = VREDIS_CAPTCHA_LINE_KEY_PREFIX + effectiveScene.name() + ":" + captchaId;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(storedCode) && effectiveScene != CaptchaScene.DEFAULT) {
            redisKey = VREDIS_CAPTCHA_LINE_KEY_PREFIX + CaptchaScene.DEFAULT.name() + ":" + captchaId;
            storedCode = redisTemplate.opsForValue().get(redisKey);
        }
        if (!StringUtils.hasText(storedCode)) {
            return false;
        }
        redisTemplate.delete(redisKey);
        return storedCode.equalsIgnoreCase(userInput.trim());
    }

}
