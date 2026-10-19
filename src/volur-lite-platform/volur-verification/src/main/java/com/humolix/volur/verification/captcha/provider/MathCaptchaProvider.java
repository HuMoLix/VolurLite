package com.humolix.volur.verification.captcha.provider;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
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
public class MathCaptchaProvider implements CaptchaProvider {

    private static final String VREDIS_CAPTCHA_MATH_KEY_PREFIX = "vredis:verification:captcha:math:";
    private static final String CAPTCHA_IMAGE_TYPE = "data:image/png;base64,";
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;

    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaType getType() {
        return CaptchaType.MATH;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        MathGenerator mathGenerator = new MathGenerator(1);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT);
        captcha.setGenerator(mathGenerator);
        captcha.createCode();

        String captchaId = IdUtil.fastSimpleUUID();
        String answer = String.valueOf(mathGenerator.verify(captcha.getCode(), "0") ? "0" : "");
        
        // Hutool MathGenerator verify method computes the result
        // Or we can parse the expression or verify via generator
        String code = captcha.getCode(); // e.g. "3+5=?"
        // Calculate result:
        String calculatedAnswer = computeMathResult(code);

        String redisKey = VREDIS_CAPTCHA_MATH_KEY_PREFIX + scene.name() + ":" + captchaId;
        redisTemplate.opsForValue().set(redisKey, calculatedAnswer, Duration.ofMinutes(2));

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(CAPTCHA_IMAGE_TYPE + captcha.getImageBase64())
                .build();
    }

    private String computeMathResult(String code) {
        if (code == null) return "0";
        String clean = code.replace("?", "").replace("=", "").trim();
        try {
            if (clean.contains("+")) {
                String[] parts = clean.split("\\+");
                return String.valueOf(Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim()));
            } else if (clean.contains("-")) {
                String[] parts = clean.split("-");
                return String.valueOf(Integer.parseInt(parts[0].trim()) - Integer.parseInt(parts[1].trim()));
            } else if (clean.contains("*") || clean.contains("x") || clean.contains("×")) {
                String[] parts = clean.split("[*x×]");
                return String.valueOf(Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim()));
            }
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 解析算术表达式失败: code={}", code, ex);
        }
        return "0";
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userInput)) {
            return false;
        }
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String redisKey = VREDIS_CAPTCHA_MATH_KEY_PREFIX + effectiveScene.name() + ":" + captchaId;
        String storedAnswer = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(storedAnswer) && effectiveScene != CaptchaScene.DEFAULT) {
            redisKey = VREDIS_CAPTCHA_MATH_KEY_PREFIX + CaptchaScene.DEFAULT.name() + ":" + captchaId;
            storedAnswer = redisTemplate.opsForValue().get(redisKey);
        }
        if (!StringUtils.hasText(storedAnswer)) {
            return false;
        }
        redisTemplate.delete(redisKey);
        return storedAnswer.equalsIgnoreCase(userInput.trim());
    }

}
