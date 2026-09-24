package com.humolix.volur.verification;

import com.humolix.volur.verification.captcha.engine.PuzzleImageLoader;
import com.humolix.volur.verification.captcha.engine.SlidePuzzleEngine;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SlidePuzzleEngineTest {

    @Test
    public void testGeneratePuzzleWithLoader() {
        PuzzleImageLoader loader = new PuzzleImageLoader();
        loader.init();
        SlidePuzzleEngine engine = new SlidePuzzleEngine(loader);
        SlidePuzzleEngine.PuzzleResult result = engine.generatePuzzle();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.backgroundImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.sliderImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.x() > 0);
        Assertions.assertTrue(result.y() > 0);
    }

    @Test
    public void testGeneratePuzzleFallbackWhenLoaderReturnsNull() {
        // 模拟图库为空或不可用时的平滑降级
        PuzzleImageLoader emptyLoader = new PuzzleImageLoader() {
            @Override
            public java.awt.image.BufferedImage loadRandomImage() {
                return null;
            }
        };
        SlidePuzzleEngine engine = new SlidePuzzleEngine(emptyLoader);
        SlidePuzzleEngine.PuzzleResult result = engine.generatePuzzle();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.backgroundImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.sliderImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.x() > 0);
        Assertions.assertTrue(result.y() > 0);
    }

    @Test
    public void testAntiDowngradeRiskResolution() {
        RiskEvaluationEngine riskEngine = new RiskEvaluationEngine(null, null, null, null);

        // 注册场景要求最低为 SLIDE_PUZZLE，即使用户请求 LINE 也必须被强制提升到 SLIDE_PUZZLE
        CaptchaType resolved = riskEngine.resolveEscalatedType(CaptchaType.LINE, RiskLevel.LOW, CaptchaScene.REGISTER);
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, resolved);

        // 改密场景同样强制 SLIDE_PUZZLE
        CaptchaType changePwdResolved = riskEngine.resolveEscalatedType(CaptchaType.MATH, RiskLevel.LOW, CaptchaScene.CHANGE_PASSWORD);
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, changePwdResolved);

        // 登录场景低风险允许 LINE
        CaptchaType loginResolved = riskEngine.resolveEscalatedType(CaptchaType.LINE, RiskLevel.LOW, CaptchaScene.LOGIN);
        Assertions.assertEquals(CaptchaType.LINE, loginResolved);

        // 登录场景中风险自动升级为 SHEAR
        CaptchaType loginMedResolved = riskEngine.resolveEscalatedType(CaptchaType.LINE, RiskLevel.MEDIUM, CaptchaScene.LOGIN);
        Assertions.assertEquals(CaptchaType.SHEAR, loginMedResolved);

        // 登录场景高风险自动升级为 SLIDE_PUZZLE
        CaptchaType loginHighResolved = riskEngine.resolveEscalatedType(CaptchaType.LINE, RiskLevel.HIGH, CaptchaScene.LOGIN);
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, loginHighResolved);
    }

    @Test
    public void testPuzzleCutoutShapeIntegrity() {
        PuzzleImageLoader loader = new PuzzleImageLoader();
        loader.init();
        SlidePuzzleEngine engine = new SlidePuzzleEngine(loader);
        SlidePuzzleEngine.PuzzleResult result = engine.generatePuzzle();

        // 验证坐标在合理视口范围内
        Assertions.assertTrue(result.x() >= 45 && result.x() <= 280, "targetX should be in valid viewport range");
        Assertions.assertTrue(result.y() >= 10 && result.y() <= 120, "targetY should be in valid viewport range");
    }

}
