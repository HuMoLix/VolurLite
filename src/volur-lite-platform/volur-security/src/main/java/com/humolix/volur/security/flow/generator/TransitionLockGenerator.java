package com.humolix.volur.security.flow.generator;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Component
public class TransitionLockGenerator {

    public static final String TL_PREFIX = "ADG-";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] HMAC_SECRET_BYTES = "VolurLiteGoogleAccountsSecretHmac2026!#$".getBytes(StandardCharsets.UTF_8);
    private static final SecretKey SECRET_KEY = new SecretKeySpec(HMAC_SECRET_BYTES, HMAC_ALGORITHM);
    private static final int REQUIRED_TOKEN_LENGTH = 60;
    private static final Pattern TL_PATTERN = Pattern.compile("^ADG-[A-Za-z0-9_-]{60}$");

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(String dsh, int cid, String step, String identifier) {
        try {
            byte[] nonceBytes = new byte[16];
            secureRandom.nextBytes(nonceBytes);
            String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

            String rawPayload = (dsh != null ? dsh : "") + "|" + cid + "|" + (step != null ? step : "")
                    + "|" + (identifier != null ? identifier : "") + "|" + System.currentTimeMillis()
                    + "|" + nonce;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(SECRET_KEY);
            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String base64Hmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

            byte[] extraBytes = new byte[24];
            secureRandom.nextBytes(extraBytes);
            String extraEntropy = Base64.getUrlEncoder().withoutPadding().encodeToString(extraBytes);

            String combined = base64Hmac + extraEntropy;
            String tokenContent = combined.substring(0, REQUIRED_TOKEN_LENGTH);
            return TL_PREFIX + tokenContent;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to calculate HMAC signature for transition lock", e);
        }
    }

    public boolean isValid(String tl) {
        if (tl == null || tl.isEmpty()) {
            return false;
        }
        return TL_PATTERN.matcher(tl).matches();
    }

}
