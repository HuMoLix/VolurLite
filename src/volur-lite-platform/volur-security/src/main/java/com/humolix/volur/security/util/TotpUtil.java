package com.humolix.volur.security.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

public final class TotpUtil {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SECRET_BYTE_LENGTH = 20;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TotpUtil() {
    }

    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public static String generateCode(String secretBase32, long timeSeconds) {
        byte[] key = decodeBase32(secretBase32);
        long counter = timeSeconds / TIME_STEP_SECONDS;
        return generateCodeForCounter(key, counter);
    }

    public static boolean verifyCode(String secretBase32, String inputCode, int windowSteps) {
        if (secretBase32 == null || inputCode == null || inputCode.length() != CODE_DIGITS) {
            return false;
        }

        byte[] key;
        try {
            key = decodeBase32(secretBase32);
        } catch (Exception e) {
            return false;
        }

        long currentSeconds = System.currentTimeMillis() / 1000L;
        long currentCounter = currentSeconds / TIME_STEP_SECONDS;

        int trimmedWindow = Math.max(0, windowSteps);
        for (int i = -trimmedWindow; i <= trimmedWindow; i++) {
            String expected = generateCodeForCounter(key, currentCounter + i);
            if (expected.equals(inputCode)) {
                return true;
            }
        }
        return false;
    }

    public static String generateOtpAuthUri(String issuer, String account, String secretBase32) {
        String cleanIssuer = issuer == null ? "Volur" : issuer.trim();
        String cleanAccount = account == null ? "user" : account.trim();
        String cleanSecret = secretBase32 == null ? "" : secretBase32.trim().replace(" ", "").toUpperCase(Locale.ROOT);

        String encodedIssuer = URLEncoder.encode(cleanIssuer, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedAccount = URLEncoder.encode(cleanAccount, StandardCharsets.UTF_8).replace("+", "%20");

        String label = encodedIssuer + ":" + encodedAccount;

        return "otpauth://totp/" + label
                + "?secret=" + cleanSecret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1"
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    private static String generateCodeForCounter(byte[] key, long counter) {
        try {
            byte[] data = new byte[8];
            long value = counter;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_SHA1_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int truncatedHash = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = truncatedHash % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;

        while (next < data.length) {
            buffer <<= 8;
            buffer |= (data[next++] & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String clean = base32.trim().replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT);
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Illegal Base32 character: " + c);
            }
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }

}
