package com.humolix.volur.framework.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DesensitizationUtilTest {

    @Test
    @DisplayName("手机号脱敏测试")
    void testMaskPhone() {
        assertEquals("138****5678", DesensitizationUtil.maskPhone("13812345678"));
        assertEquals("139****1234", DesensitizationUtil.maskPhone(" 13987651234 "));
        assertEquals("123****7890", DesensitizationUtil.maskPhone("1234567890"));
        assertEquals("***", DesensitizationUtil.maskPhone("12345"));
        assertNull(DesensitizationUtil.maskPhone(null));
        assertEquals("", DesensitizationUtil.maskPhone(""));
        assertEquals("   ", DesensitizationUtil.maskPhone("   "));
    }

    @Test
    @DisplayName("身份证号脱敏测试")
    void testMaskIdCard() {
        assertEquals("110101********1234", DesensitizationUtil.maskIdCard("110101199001011234"));
        assertEquals("110101******123", DesensitizationUtil.maskIdCard("110101900101123"));
        assertEquals("1101****1234", DesensitizationUtil.maskIdCard("110188881234"));
        assertEquals("***", DesensitizationUtil.maskIdCard("12345"));
        assertNull(DesensitizationUtil.maskIdCard(null));
        assertEquals("", DesensitizationUtil.maskIdCard(""));
    }

    @Test
    @DisplayName("电子邮箱脱敏测试")
    void testMaskEmail() {
        assertEquals("z***n@example.com", DesensitizationUtil.maskEmail("zhangsan@example.com"));
        assertEquals("a***@example.com", DesensitizationUtil.maskEmail("ab@example.com"));
        assertEquals("a***@example.com", DesensitizationUtil.maskEmail("a@example.com"));
        assertEquals("***", DesensitizationUtil.maskEmail("invalid-email"));
        assertNull(DesensitizationUtil.maskEmail(null));
        assertEquals("", DesensitizationUtil.maskEmail(""));
    }

    @Test
    @DisplayName("真实姓名脱敏测试")
    void testMaskRealName() {
        assertEquals("*", DesensitizationUtil.maskRealName("张"));
        assertEquals("张*", DesensitizationUtil.maskRealName("张三"));
        assertEquals("李*五", DesensitizationUtil.maskRealName("李四五"));
        assertEquals("诸**明", DesensitizationUtil.maskRealName("诸葛孔明"));
        assertEquals("欧***起", DesensitizationUtil.maskRealName("欧阳长风起"));
        assertNull(DesensitizationUtil.maskRealName(null));
        assertEquals("", DesensitizationUtil.maskRealName(""));
    }

    @Test
    @DisplayName("银行卡脱敏测试")
    void testMaskBankCard() {
        assertEquals("622202**********1234", DesensitizationUtil.maskBankCard("62220212345678901234"));
        assertEquals("622202******1234", DesensitizationUtil.maskBankCard("622202 123456 1234"));
        assertEquals("***", DesensitizationUtil.maskBankCard("123456789"));
        assertNull(DesensitizationUtil.maskBankCard(null));
    }

    @Test
    @DisplayName("密钥脱敏测试")
    void testMaskKey() {
        assertEquals("akid********1234", DesensitizationUtil.maskKey("akid12345678901234"));
        assertEquals("********", DesensitizationUtil.maskKey("short"));
        assertNull(DesensitizationUtil.maskKey(null));
    }

    @Test
    @DisplayName("通用字符串掩码测试")
    void testGenericMask() {
        assertEquals("ab**ef", DesensitizationUtil.mask("abcdef", 2, 2, '*'));
        assertEquals("a##d", DesensitizationUtil.mask("abcd", 1, 1, '#'));
        assertEquals("abcdef", DesensitizationUtil.mask("abcdef", 3, 3));
        assertEquals("abcdef", DesensitizationUtil.mask("abcdef", 4, 3));
        assertNull(DesensitizationUtil.mask(null, 1, 1));
    }

}
