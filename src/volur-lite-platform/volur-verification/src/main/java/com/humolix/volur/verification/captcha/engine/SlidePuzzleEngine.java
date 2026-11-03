package com.humolix.volur.verification.captcha.engine;

import cn.hutool.core.codec.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlidePuzzleEngine {

    public static final int CANVAS_WIDTH = PuzzleImageLoader.TARGET_WIDTH;
    public static final int CANVAS_HEIGHT = PuzzleImageLoader.TARGET_HEIGHT;
    private static final int BLOCK_SIZE = 45;
    private static final int BLOCK_RADIUS = 8;
    private static final String BASE64_PREFIX = "data:image/png;base64,";

    private final PuzzleImageLoader puzzleImageLoader;

    public record PuzzleResult(String backgroundImageBase64, String sliderImageBase64, int x, int y) {
    }

    public PuzzleResult generatePuzzle() {
        BufferedImage backgroundCanvas = puzzleImageLoader.loadRandomImage();
        if (backgroundCanvas == null) {
            backgroundCanvas = generateRandomBackground(CANVAS_WIDTH, CANVAS_HEIGHT);
        }

        int targetX = ThreadLocalRandom.current().nextInt(BLOCK_SIZE + 15, CANVAS_WIDTH - BLOCK_SIZE - 20);
        int targetY = ThreadLocalRandom.current().nextInt(BLOCK_RADIUS * 2 + 5, CANVAS_HEIGHT - BLOCK_SIZE - 15);

        BufferedImage sliderBlock = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sliderG2d = sliderBlock.createGraphics();
        sliderG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape puzzleShape = createPuzzleShape(0, targetY, BLOCK_SIZE, BLOCK_RADIUS);

        sliderG2d.setClip(puzzleShape);
        sliderG2d.drawImage(backgroundCanvas, -targetX, 0, null);

        sliderG2d.setClip(null);
        sliderG2d.setColor(new Color(255, 255, 255, 220));
        sliderG2d.setStroke(new BasicStroke(2.0f));
        sliderG2d.draw(puzzleShape);
        sliderG2d.dispose();

        Graphics2D bgG2d = backgroundCanvas.createGraphics();
        bgG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape bgCutoutShape = createPuzzleShape(targetX, targetY, BLOCK_SIZE, BLOCK_RADIUS);

        bgG2d.setColor(new Color(0, 0, 0, 160));
        bgG2d.fill(bgCutoutShape);
        bgG2d.setColor(new Color(255, 255, 255, 160));
        bgG2d.setStroke(new BasicStroke(1.5f));
        bgG2d.draw(bgCutoutShape);
        bgG2d.dispose();

        String bgBase64 = imageToBase64(backgroundCanvas);
        String sliderBase64 = imageToBase64(sliderBlock);

        return new PuzzleResult(bgBase64, sliderBase64, targetX, targetY);
    }

    private BufferedImage generateRandomBackground(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color c1 = new Color(ThreadLocalRandom.current().nextInt(30, 90),
                ThreadLocalRandom.current().nextInt(50, 120),
                ThreadLocalRandom.current().nextInt(100, 180));
        Color c2 = new Color(ThreadLocalRandom.current().nextInt(80, 150),
                ThreadLocalRandom.current().nextInt(130, 200),
                ThreadLocalRandom.current().nextInt(180, 250));

        GradientPaint gradient = new GradientPaint(0, 0, c1, width, height, c2);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        for (int i = 0; i < 8; i++) {
            g2d.setColor(new Color(255, 255, 255, ThreadLocalRandom.current().nextInt(20, 50)));
            int x = ThreadLocalRandom.current().nextInt(0, width);
            int y = ThreadLocalRandom.current().nextInt(0, height);
            int r = ThreadLocalRandom.current().nextInt(20, 80);
            g2d.fillOval(x, y, r, r);
        }

        for (int i = 0; i < 15; i++) {
            g2d.setColor(new Color(ThreadLocalRandom.current().nextInt(200, 255),
                    ThreadLocalRandom.current().nextInt(200, 255),
                    ThreadLocalRandom.current().nextInt(200, 255),
                    ThreadLocalRandom.current().nextInt(40, 90)));
            int x1 = ThreadLocalRandom.current().nextInt(0, width);
            int y1 = ThreadLocalRandom.current().nextInt(0, height);
            int x2 = ThreadLocalRandom.current().nextInt(0, width);
            int y2 = ThreadLocalRandom.current().nextInt(0, height);
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.dispose();
        return image;
    }

    private Shape createPuzzleShape(int x, int y, int size, int r) {
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);

        // Top edge: line -> tab up -> line
        path.lineTo(x + (size - 2 * r) / 2.0, y);
        path.quadTo(x + size / 2.0, y - r * 1.5, x + (size + 2 * r) / 2.0, y);
        path.lineTo(x + size, y);

        // Right edge: line -> tab right -> line
        path.lineTo(x + size, y + (size - 2 * r) / 2.0);
        path.quadTo(x + size + r * 1.5, y + size / 2.0, x + size, y + (size + 2 * r) / 2.0);
        path.lineTo(x + size, y + size);

        // Bottom edge: line -> notch up (inward) -> line
        path.lineTo(x + (size + 2 * r) / 2.0, y + size);
        path.quadTo(x + size / 2.0, y + size - r * 1.5, x + (size - 2 * r) / 2.0, y + size);
        path.lineTo(x, y + size);

        // Left edge: line -> notch right (inward) -> line
        path.lineTo(x, y + (size + 2 * r) / 2.0);
        path.quadTo(x + r * 1.5, y + size / 2.0, x, y + (size - 2 * r) / 2.0);
        path.lineTo(x, y);

        path.closePath();
        return path;
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(16384)) {
            ImageIO.write(image, "PNG", os);
            return BASE64_PREFIX + Base64.encode(os.toByteArray());
        } catch (IOException e) {
            log.error("[Volur-Verification] 图像 Base64 编码失败", e);
            return "";
        }
    }

}
