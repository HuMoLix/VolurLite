package com.humolix.volur.security.flow;

import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TransitionLockGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] HMAC_SECRET_BYTES = "VolurLiteGoogleAccountsSecretHmac2026!#$".getBytes(StandardCharsets.UTF_8);

    public String generate(String dsh, int cid, String step, String identifier) {
        try {
            String rawPayload = (dsh != null ? dsh : "") + "|" + cid + "|" + (step != null ? step : "")
                    + "|" + (identifier != null ? identifier : "") + "|" + System.currentTimeMillis()
                    + "|" + RandomUtil.randomString(16);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(HMAC_SECRET_BYTES, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

            String randomExtra = RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-", 32);
            String combined = base64Url + randomExtra;
            String tokenContent = combined.length() >= 60 ? combined.substring(0, 60) : combined + RandomUtil.randomString(60 - combined.length());
            return "ADG-" + tokenContent;
        } catch (Exception e) {
            return "ADG-" + RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-", 60);
        }
    }

}
