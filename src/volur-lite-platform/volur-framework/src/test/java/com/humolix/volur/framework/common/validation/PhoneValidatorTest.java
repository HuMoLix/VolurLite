package com.humolix.volur.framework.common.validation;

import com.humolix.volur.framework.common.validation.annotation.Phone;
import com.humolix.volur.framework.common.validation.enums.ChinaCarrier;
import com.humolix.volur.framework.common.validation.validator.PhoneValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

class PhoneValidatorTest {

    private PhoneValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhoneValidator();
        validator.initialize(new Phone() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Phone.class;
            }

            @Override
            public String message() {
                return "手机号码格式不正确";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public ChinaCarrier[] carriers() {
                return new ChinaCarrier[]{ChinaCarrier.ALL};
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "13800138000", "13512345678", "13401234567", "13481234567",
            "15012345678", "15812345678", "17812345678", "18812345678", "19812345678"
    })
    @DisplayName("中国移动号段校验通过并准确识别运营商")
    void testChinaMobile(String phone) {
        assertTrue(PhoneValidator.isValidPhone(phone));
        assertTrue(validator.isValid(phone, null));
        assertEquals(ChinaCarrier.CHINA_MOBILE, PhoneValidator.detectCarrier(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "13012345678", "13112345678", "13212345678", "15512345678",
            "15612345678", "16612345678", "17612345678", "18612345678", "19612345678"
    })
    @DisplayName("中国联通号段校验通过并准确识别运营商")
    void testChinaUnicom(String phone) {
        assertTrue(PhoneValidator.isValidPhone(phone));
        assertTrue(validator.isValid(phone, null));
        assertEquals(ChinaCarrier.CHINA_UNICOM, PhoneValidator.detectCarrier(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "13312345678", "13491234567", "14912345678", "15312345678",
            "17712345678", "18012345678", "18912345678", "19112345678", "19912345678"
    })
    @DisplayName("中国电信号段校验通过并准确识别运营商")
    void testChinaTelecom(String phone) {
        assertTrue(PhoneValidator.isValidPhone(phone));
        assertTrue(validator.isValid(phone, null));
        assertEquals(ChinaCarrier.CHINA_TELECOM, PhoneValidator.detectCarrier(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "19200000000", "19212345678", "19299999999"
    })
    @DisplayName("中国广电号段校验通过并准确识别运营商")
    void testChinaBroadnet(String phone) {
        assertTrue(PhoneValidator.isValidPhone(phone));
        assertTrue(validator.isValid(phone, null));
        assertEquals(ChinaCarrier.CHINA_BROADNET, PhoneValidator.detectCarrier(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "17001234567", "17051234567", "17101234567", // 170/171 虚拟运营商号段
            "16212345678", "16512345678", "16712345678", // 162/165/167 虚拟运营商号段
            "12345678901", "10086111111",                 // 非四大运营商公众手机号段
            "1380013800",                                 // 10 位长度不足
            "138001380001",                               // 12 位长度超长
            "1380013800a",                                // 包含非数字
            "23800138000"                                 // 非 1 开头
    })
    @DisplayName("非四大运营商及非法号码拦截")
    void testInvalidPhones(String phone) {
        assertFalse(PhoneValidator.isValidPhone(phone));
        assertFalse(validator.isValid(phone, null));
        assertNull(PhoneValidator.detectCarrier(phone));
    }

    @Test
    @DisplayName("指定特定运营商范围过滤校验")
    void testCarrierScopeFiltering() {
        String mobilePhone = "13800138000";
        String unicomPhone = "18600000000";

        // 仅允许中国移动
        assertTrue(PhoneValidator.isValidPhone(mobilePhone, ChinaCarrier.CHINA_MOBILE));
        assertFalse(PhoneValidator.isValidPhone(unicomPhone, ChinaCarrier.CHINA_MOBILE));

        // 允许移动和联通
        assertTrue(PhoneValidator.isValidPhone(mobilePhone, ChinaCarrier.CHINA_MOBILE, ChinaCarrier.CHINA_UNICOM));
        assertTrue(PhoneValidator.isValidPhone(unicomPhone, ChinaCarrier.CHINA_MOBILE, ChinaCarrier.CHINA_UNICOM));
    }

    @Test
    @DisplayName("空值与空白字符按照 Bean Validation 规范放行 (由 @NotBlank 约束必填)")
    void testNullAndEmpty() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }
}
