package com.humolix.volur.verification;

import com.humolix.volur.verification.captcha.engine.SlidePuzzleEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SlidePuzzleEngineTest {

    @Test
    public void testGeneratePuzzle() {
        SlidePuzzleEngine engine = new SlidePuzzleEngine();
        SlidePuzzleEngine.PuzzleResult result = engine.generatePuzzle();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.backgroundImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.sliderImageBase64().startsWith("data:image/png;base64,"));
        Assertions.assertTrue(result.x() > 0);
        Assertions.assertTrue(result.y() > 0);
    }

}
