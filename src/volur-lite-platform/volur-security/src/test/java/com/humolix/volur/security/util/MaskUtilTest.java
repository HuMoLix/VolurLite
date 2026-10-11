package com.humolix.volur.security.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MaskUtilTest {

    @Test
    void testIdentifierCardMask() {
        Assertions.assertEquals("110101********1234", MaskUtil.identifierCardMask("110101199003071234"));
        Assertions.assertEquals("110101********1234", MaskUtil.IdentifierCardMask("110101199003071234"));
        Assertions.assertEquals("110101******123", MaskUtil.identifierCardMask("110101900307123"));
        Assertions.assertEquals("1101****1234", MaskUtil.identifierCardMask("110188881234"));
        Assertions.assertEquals("***", MaskUtil.identifierCardMask("12345"));
        Assertions.assertNull(MaskUtil.identifierCardMask(null));
        Assertions.assertEquals("", MaskUtil.identifierCardMask(""));
        Assertions.assertEquals("", MaskUtil.identifierCardMask("   "));
    }

    @Test
    void testPhoneMask() {
        Assertions.assertEquals("138****5678", MaskUtil.phoneMask("13812345678"));
        Assertions.assertEquals("138****5678", MaskUtil.PhoneMask("13812345678"));
        Assertions.assertEquals("138****6789", MaskUtil.phoneMask("138123456789"));
        Assertions.assertEquals("***", MaskUtil.phoneMask("12345"));
        Assertions.assertEquals("***", MaskUtil.phoneMask("123"));
        Assertions.assertNull(MaskUtil.phoneMask(null));
        Assertions.assertEquals("", MaskUtil.phoneMask(""));
        Assertions.assertEquals("", MaskUtil.phoneMask("  "));
    }

    @Test
    void testEmailMask() {
        Assertions.assertEquals("z***n@example.com", MaskUtil.emailMask("zhangsan@example.com"));
        Assertions.assertEquals("z***n@example.com", MaskUtil.EmailMask("zhangsan@example.com"));
        Assertions.assertEquals("a***@example.com", MaskUtil.emailMask("ab@example.com"));
        Assertions.assertEquals("a***@example.com", MaskUtil.emailMask("a@example.com"));
        Assertions.assertEquals("***", MaskUtil.emailMask("invalid-email"));
        Assertions.assertNull(MaskUtil.emailMask(null));
        Assertions.assertEquals("", MaskUtil.emailMask(""));
        Assertions.assertEquals("", MaskUtil.emailMask("   "));
    }

    @Test
    void testNameMask() {
        Assertions.assertEquals("*", MaskUtil.nameMask("张"));
        Assertions.assertEquals("*", MaskUtil.NameMask("张"));
        Assertions.assertEquals("张*", MaskUtil.nameMask("张三"));
        Assertions.assertEquals("李*五", MaskUtil.nameMask("李四五"));
        Assertions.assertEquals("诸**明", MaskUtil.nameMask("诸葛孔明"));
        Assertions.assertEquals("欧***起", MaskUtil.nameMask("欧阳长风起"));
        Assertions.assertNull(MaskUtil.nameMask(null));
        Assertions.assertEquals("", MaskUtil.nameMask(""));
        Assertions.assertEquals("", MaskUtil.nameMask("   "));
    }

    @Test
    void testBankCardMask() {
        Assertions.assertEquals("622202**********1234", MaskUtil.bankCardMask("62220212345678901234"));
        Assertions.assertEquals("622202******1234", MaskUtil.BankCardMask("622202 123456 1234"));
        Assertions.assertEquals("***", MaskUtil.bankCardMask("123456789"));
        Assertions.assertNull(MaskUtil.bankCardMask(null));
        Assertions.assertEquals("", MaskUtil.bankCardMask(""));
    }

    @Test
    void testKeyMask() {
        Assertions.assertEquals("akid******1234", MaskUtil.keyMask("akid1234561234"));
        Assertions.assertEquals("********", MaskUtil.KeyMask("short"));
        Assertions.assertNull(MaskUtil.keyMask(null));
        Assertions.assertEquals("", MaskUtil.keyMask(""));
    }

    @Test
    void testGenericMask() {
        Assertions.assertEquals("ab**ef", MaskUtil.genericMask("abcdef", 2, 2, '*'));
        Assertions.assertEquals("ab**ef", MaskUtil.GenericMask("abcdef", 2, 2, '*'));
        Assertions.assertEquals("a##d", MaskUtil.genericMask("abcd", 1, 1, '#'));
        Assertions.assertEquals("abcdef", MaskUtil.genericMask("abcdef", 3, 3, '*'));
        Assertions.assertEquals("abcdef", MaskUtil.genericMask("abcdef", 4, 3, '*'));
        Assertions.assertNull(MaskUtil.genericMask(null, 1, 1, '*'));
    }

}
