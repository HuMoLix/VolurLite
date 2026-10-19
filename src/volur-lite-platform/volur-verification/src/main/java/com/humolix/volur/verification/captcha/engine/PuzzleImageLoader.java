package com.humolix.volur.verification.captcha.engine;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Slf4j
@Component
public class PuzzleImageLoader {

    public static final int TARGET_WIDTH = 320;
    public static final int TARGET_HEIGHT = 160;
    private static final int MAX_CACHED_IMAGES = 50;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");
    private static final String CLASSPATH_PUZZLE_PATTERN = "classpath*:captcha/puzzle/*.*";

    @Value("${volur.captcha.puzzle.custom-path:}")
    private String customPath;

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final List<BufferedImage> preScaledPool = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        reloadImagePool();
    }

    public synchronized void reloadImagePool() {
        List<BufferedImage> newPool = new ArrayList<>();

        if (StringUtils.hasText(customPath)) {
            loadFromCustomPath(newPool);
        }

        if (newPool.isEmpty()) {
            loadFromClasspath(newPool);
        }

        preScaledPool.clear();
        preScaledPool.addAll(newPool);
        log.info("[Volur-Verification] 滑块拼图内存池预热完成，就绪背景图数量: {} 张 (内存占用: ~{} KB)",
                preScaledPool.size(), preScaledPool.size() * (TARGET_WIDTH * TARGET_HEIGHT * 3) / 1024);
    }

    public BufferedImage loadRandomImage() {
        if (preScaledPool.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(preScaledPool.size());
        BufferedImage template = preScaledPool.get(index);
        return copyImage(template);
    }

    private void loadFromCustomPath(List<BufferedImage> pool) {
        try {
            Path dir = Paths.get(customPath.trim());
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                List<File> files = stream
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .filter(this::isSupportedImage)
                        .limit(MAX_CACHED_IMAGES)
                        .toList();

                for (File file : files) {
                    try {
                        BufferedImage raw = ImageIO.read(file);
                        if (raw != null) {
                            pool.add(adaptAndScale(raw));
                        }
                    } catch (Exception e) {
                        log.warn("[Volur-Verification] 预加载外部图片失败: file={}, reason={}", file.getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 扫描外部自定义图库失败: path={}, reason={}", customPath, ex.getMessage());
        }
    }

    private void loadFromClasspath(List<BufferedImage> pool) {
        try {
            Resource[] resources = resourceResolver.getResources(CLASSPATH_PUZZLE_PATTERN);
            List<Resource> validResources = Arrays.stream(resources)
                    .filter(Resource::isReadable)
                    .filter(r -> isSupportedFilename(r.getFilename()))
                    .limit(MAX_CACHED_IMAGES)
                    .toList();

            for (Resource resource : validResources) {
                try (InputStream in = resource.getInputStream()) {
                    BufferedImage raw = ImageIO.read(in);
                    if (raw != null) {
                        pool.add(adaptAndScale(raw));
                    }
                } catch (Exception e) {
                    log.warn("[Volur-Verification] 预加载 Classpath 内置图片失败: file={}, reason={}", resource.getFilename(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("[Volur-Verification] Classpath 内置图库扫描失败: {}", ex.getMessage());
        }
    }

    private BufferedImage adaptAndScale(BufferedImage source) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        double targetAspect = (double) TARGET_WIDTH / TARGET_HEIGHT;
        double srcAspect = (double) srcW / srcH;

        int cropW;
        int cropH;
        int cropX;
        int cropY;

        if (srcAspect > targetAspect) {
            cropH = srcH;
            cropW = (int) Math.round(srcH * targetAspect);
            cropX = (srcW - cropW) / 2;
            cropY = 0;
        } else {
            cropW = srcW;
            cropH = (int) Math.round(srcW / targetAspect);
            cropX = 0;
            cropY = (srcH - cropH) / 2;
        }

        BufferedImage cropped = source.getSubimage(cropX, cropY, cropW, cropH);
        BufferedImage scaled = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(cropped, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        g2d.dispose();
        return scaled;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    private boolean isSupportedImage(File file) {
        return isSupportedFilename(file.getName());
    }

    private boolean isSupportedFilename(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

}
