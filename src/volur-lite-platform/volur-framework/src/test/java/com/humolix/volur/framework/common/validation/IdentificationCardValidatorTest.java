package com.humolix.volur.framework.common.validation;

import com.humolix.volur.framework.common.validation.validator.IdentificationCardValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IdentificationCardValidatorTest {

    private final IdentificationCardValidator validator = new IdentificationCardValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "110101199003072367", // 北京，末位为数字 7
            "440305199510240217", // 广东深圳，末位为数字 7
            "11010119900301129X", // 末位为大写 X
            "11010119900301129x"  // 末位为小写 x
    })
    @DisplayName("有效二代居民身份证测试通过")
    void testValidIdCards(String idCard) {
        assertTrue(IdentificationCardValidator.isValidIdCard(idCard));
        assertTrue(validator.isValid(idCard, null));
    }

    @Test
    @DisplayName("提取二代身份证中的出生日期与性别")
    void testExtractInfo() {
        String idCard = "11010119900301129X";
        LocalDate birthDate = IdentificationCardValidator.extractBirthDate(idCard);
        assertNotNull(birthDate);
        assertEquals(LocalDate.of(1990, 3, 1), birthDate);

        Boolean isMale = IdentificationCardValidator.isMale(idCard);
        assertNotNull(isMale);
        assertTrue(isMale); // 第 17 位为 9 (奇数)，男性

        String femaleId = "110101199003072367";
        Boolean isFemale = IdentificationCardValidator.isMale(femaleId);
        assertNotNull(isFemale);
        assertFalse(isFemale); // 第 17 位为 6 (偶数)，女性
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "110101900301001",    // 15 位一代身份证（只考虑第二代身份证）
            "11010119900301001",  // 17 位缺失
            "1101011990030100112",// 19 位过长
            "11010119900301001A", // 非法字符 A
            "000101199003010011", // 非法省份代码 00
            "990101199003010011", // 非法省份代码 99
            "110101201902290012", // 2019 年非平年无 2 月 29 日
            "110101199013010018", // 非法月份 13
            "110101199001320011", // 非法日期 32
            "110101188001010011", // 1900 年以前历史超期年份
            "110101209901010011", // 未来年份
            "110101199003010010"  // 校验码不匹配 (应为 X，实际为 0)
    })
    @DisplayName("无效二代身份证测试拦截")
    void testInvalidIdCards(String idCard) {
        assertFalse(IdentificationCardValidator.isValidIdCard(idCard));
        assertFalse(validator.isValid(idCard, null));
    }

    @Test
    @DisplayName("空值与空白字符按照 Bean Validation 规范放行 (由 @NotBlank 约束必填)")
    void testNullAndEmpty() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }
}
