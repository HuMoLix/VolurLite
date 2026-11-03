package com.humolix.volur.verification;

import com.humolix.volur.verification.risk.enums.CaptchaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CaptchaTypeTest {

    @Test
    void fromString_standardNames_matchCorrectly() {
        Assertions.assertEquals(CaptchaType.LINE, CaptchaType.fromString("LINE"));
        Assertions.assertEquals(CaptchaType.LINE, CaptchaType.fromString("line"));
        Assertions.assertEquals(CaptchaType.MATH, CaptchaType.fromString("MATH"));
        Assertions.assertEquals(CaptchaType.SHEAR, CaptchaType.fromString("SHEAR"));
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("SLIDE_PUZZLE"));
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("slide_puzzle"));
    }

    @Test
    void fromString_aliases_matchSlidePuzzleAndLine() {
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("SLIDER"));
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("slider"));
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("PUZZLE"));
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, CaptchaType.fromString("puzzle"));
        Assertions.assertEquals(CaptchaType.LINE, CaptchaType.fromString("IMAGE"));
        Assertions.assertEquals(CaptchaType.LINE, CaptchaType.fromString("image"));
    }

    @Test
    void fromString_invalidOrBlank_returnsNull() {
        Assertions.assertNull(CaptchaType.fromString(null));
        Assertions.assertNull(CaptchaType.fromString(""));
        Assertions.assertNull(CaptchaType.fromString("   "));
        Assertions.assertNull(CaptchaType.fromString("UNKNOWN"));
    }

}
