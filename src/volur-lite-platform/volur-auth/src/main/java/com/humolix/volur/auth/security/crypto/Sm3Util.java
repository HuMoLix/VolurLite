package com.humolix.volur.auth.security.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class Sm3Util {

    public static String hash(String data) {
        if (data == null) {
            return null;
        }
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String hash(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        SM3Digest digest = new SM3Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] hashed = new byte[digest.getDigestSize()];
        digest.doFinal(hashed, 0);
        return Hex.toHexString(hashed);
    }

    public static String hashWithSalt(String data, String salt, String username) {
        if (data == null || salt == null || username == null) {
            throw new IllegalArgumentException("Data, salt, and username must not be null");
        }

        String saltedData = data +
                salt +
                username;
        return hash(saltedData);
    }

}
