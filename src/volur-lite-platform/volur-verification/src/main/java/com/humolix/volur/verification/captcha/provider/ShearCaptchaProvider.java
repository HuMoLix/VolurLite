package com.humolix.volur.verification.captcha.provider;

import cn.hutool.core.codec.Base64;
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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShearCaptchaProvider implements CaptchaProvider {

    private static final String VREDIS_CAPTCHA_SHEAR_KEY_PREFIX = "vredis:verification:captcha:shear:";
    private static final String BASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CAPTCHA_IMAGE_TYPE = "data:image/png;base64,";
    private static final int CAPTCHA_LENGTH = 6;
    private static final int WIDTH = 140;
    private static final int HEIGHT = 50;

    private final StringRedisTemplate redisTemplate;

    @Override
    public CaptchaType getType() {
        return CaptchaType.SHEAR;
    }

    @Override
    public CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel) {
        String code = generateRandomCode();
        String captchaId = IdUtil.fastSimpleUUID();

        String redisKey = VREDIS_CAPTCHA_SHEAR_KEY_PREFIX + scene.name() + ":" + captchaId;
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(2));

        String imageBase64 = renderShearCaptchaImage(code);

        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(getType().name())
                .imageBase64(imageBase64)
                .build();
    }

    @Override
    public boolean verify(String captchaId, String userInput, CaptchaScene scene) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userInput)) {
            return false;
        }
        CaptchaScene effectiveScene = scene != null ? scene : CaptchaScene.DEFAULT;
        String redisKey = VREDIS_CAPTCHA_SHEAR_KEY_PREFIX + effectiveScene.name() + ":" + captchaId;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(storedCode) && effectiveScene != CaptchaScene.DEFAULT) {
            redisKey = VREDIS_CAPTCHA_SHEAR_KEY_PREFIX + CaptchaScene.DEFAULT.name() + ":" + captchaId;
            storedCode = redisTemplate.opsForValue().get(redisKey);
        }
        if (!StringUtils.hasText(storedCode)) {
            return false;
        }
        redisTemplate.delete(redisKey);
        return storedCode.equalsIgnoreCase(userInput.trim());
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(BASE_CHARS.charAt(random.nextInt(BASE_CHARS.length())));
        }
        return sb.toString();
    }

    private String renderShearCaptchaImage(String code) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Color bgColor = new Color(245, 247, 250);

        BufferedImage source = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = source.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 28);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int charWidth = (WIDTH - 20) / CAPTCHA_LENGTH;
        int baseAscent = (HEIGHT + fm.getAscent() - fm.getDescent()) / 2;

        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            g2d.setColor(new Color(random.nextInt(20, 90), random.nextInt(20, 90), random.nextInt(20, 90)));
            int x = 10 + i * charWidth + random.nextInt(-2, 3);
            int y = baseAscent + random.nextInt(-3, 4);
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        for (int i = 0; i < 4; i++) {
            g2d.setColor(new Color(random.nextInt(160, 210), random.nextInt(160, 210), random.nextInt(160, 210)));
            g2d.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        g2d.dispose();

        int[] srcPixels = new int[WIDTH * HEIGHT];
        source.getRGB(0, 0, WIDTH, HEIGHT, srcPixels, 0, WIDTH);

        int[] dstPixels = new int[WIDTH * HEIGHT];
        Arrays.fill(dstPixels, bgColor.getRGB());

        double amplitudeX = random.nextDouble(2.5, 4.5);
        double periodX = random.nextDouble(18.0, 26.0);
        double phaseX = random.nextDouble(0, 2 * Math.PI);

        double amplitudeY = random.nextDouble(1.5, 3.5);
        double periodY = random.nextDouble(30.0, 45.0);
        double phaseY = random.nextDouble(0, 2 * Math.PI);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int srcX = (int) Math.round(x + amplitudeX * Math.sin(2 * Math.PI * y / periodX + phaseX));
                int srcY = (int) Math.round(y + amplitudeY * Math.cos(2 * Math.PI * x / periodY + phaseY));
                if (srcX >= 0 && srcX < WIDTH && srcY >= 0 && srcY < HEIGHT) {
                    dstPixels[y * WIDTH + x] = srcPixels[srcY * WIDTH + srcX];
                }
            }
        }

        BufferedImage dest = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        dest.setRGB(0, 0, WIDTH, HEIGHT, dstPixels, 0, WIDTH);

        Graphics2D destG2d = dest.createGraphics();
        destG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 50; i++) {
            destG2d.setColor(new Color(random.nextInt(160, 230), random.nextInt(160, 230), random.nextInt(160, 230)));
            int nx = random.nextInt(WIDTH);
            int ny = random.nextInt(HEIGHT);
            destG2d.fillRect(nx, ny, 1, 1);
        }
        destG2d.dispose();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream(8192)) {
            ImageIO.write(dest, "PNG", os);
            return CAPTCHA_IMAGE_TYPE + Base64.encode(os.toByteArray());
        } catch (IOException e) {
            log.error("[Volur-Verification] 生成扭曲验证码图像失败", e);
            return "";
        }
    }

}
