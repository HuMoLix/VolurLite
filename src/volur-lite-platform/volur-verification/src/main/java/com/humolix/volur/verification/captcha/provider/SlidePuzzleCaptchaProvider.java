package com.humolix.volur.verification.captcha.provider;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.verification.captcha.engine.SlidePuzzleEngine;
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
public class SlidePuzzleCaptchaProvider implements CaptchaProvider {

    private static final String VREDIS_CAPTCHA_SLIDE_KEY_PREFIX = "vredis:verification:captcha:slide:";
    private static final int TOLERANCE_PIXELS = 6;

    private final SlidePuzzleEngine slidePuzzleEngine;
    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaType getType() {
        return CaptchaType.SLIDE_PUZZLE;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        SlidePuzzleEngine.PuzzleResult puzzle = slidePuzzleEngine.generatePuzzle();
        String captchaId = IdUtil.fastSimpleUUID();

        String redisKey = VREDIS_CAPTCHA_SLIDE_KEY_PREFIX + scene.name() + ":" + captchaId;
        redisTemplate.opsForValue().set(redisKey, String.valueOf(puzzle.x()), Duration.ofMinutes(2));

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(puzzle.backgroundImageBase64())
                .sliderBase64(puzzle.sliderImageBase64())
                .yPosition(puzzle.y())
                .build();
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userInput)) {
            return false;
        }

        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String redisKey = VREDIS_CAPTCHA_SLIDE_KEY_PREFIX + effectiveScene.name() + ":" + captchaId;
        String storedX = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(storedX) && effectiveScene != CaptchaScene.DEFAULT) {
            redisKey = VREDIS_CAPTCHA_SLIDE_KEY_PREFIX + CaptchaScene.DEFAULT.name() + ":" + captchaId;
            storedX = redisTemplate.opsForValue().get(redisKey);
        }
        if (!StringUtils.hasText(storedX)) {
            return false;
        }

        redisTemplate.delete(redisKey);

        try {
            int targetX = Integer.parseInt(storedX.trim());
            double userX = Double.parseDouble(userInput.trim());
            return Math.abs(userX - targetX) <= TOLERANCE_PIXELS;
        } catch (NumberFormatException ex) {
            log.warn("[Volur-Verification] 滑块位移坐标解析失败: input={}", userInput);
            return false;
        }
    }

}
