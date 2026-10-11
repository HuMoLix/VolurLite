package com.humolix.volur.security.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TotpUtilTest {

    @Test
    void generateSecret_shouldReturnValidBase32String() {
        String secret = TotpUtil.generateSecret();
        Assertions.assertNotNull(secret);
        Assertions.assertEquals(32, secret.length());
        Assertions.assertTrue(secret.matches("^[A-Z2-7]+$"));
    }

    @Test
    void generateCodeAndVerifyCode_shouldSucceed() {
        String secret = TotpUtil.generateSecret();
        long now = System.currentTimeMillis() / 1000L;
        String code = TotpUtil.generateCode(secret, now);

        Assertions.assertNotNull(code);
        Assertions.assertEquals(6, code.length());
        Assertions.assertTrue(code.matches("^\\d{6}$"));

        boolean valid = TotpUtil.verifyCode(secret, code, 1);
        Assertions.assertTrue(valid);

        boolean invalid = TotpUtil.verifyCode(secret, "000000".equals(code) ? "999999" : "000000", 1);
        Assertions.assertFalse(invalid);
    }

    @Test
    void generateOtpAuthUri_shouldFormatCorrectly() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = TotpUtil.generateOtpAuthUri("Volur", "alice@example.com", secret);

        Assertions.assertTrue(uri.startsWith("otpauth://totp/Volur:alice%40example.com?secret=JBSWY3DPEHPK3PXP"));
        Assertions.assertTrue(uri.contains("issuer=Volur"));
        Assertions.assertTrue(uri.contains("algorithm=SHA1"));
        Assertions.assertTrue(uri.contains("digits=6"));
        Assertions.assertTrue(uri.contains("period=30"));
    }

}
