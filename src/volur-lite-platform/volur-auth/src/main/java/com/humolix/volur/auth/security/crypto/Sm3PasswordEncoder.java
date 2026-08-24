package com.humolix.volur.auth.security.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

@Component
public class Sm3PasswordEncoder implements PasswordEncoder {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final String PREFIX = "pbkdf2_sm3";

    @Override
    public String encode(CharSequence rawPassword) {
        String rawPwd = Objects.requireNonNull(rawPassword).toString();
        if (!StringUtils.hasText(rawPwd)) {
            throw new IllegalArgumentException("The raw password must not be empty");
        }
        byte[] saltBytes = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltHex = Hex.toHexString(saltBytes);
        String hashHex = pbkdf2Sm3(rawPwd, saltHex, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + saltHex + "$" + hashHex;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword) || !encodedPassword.startsWith(PREFIX + "$")) {
            return false;
        }
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            String saltHex = parts[2];
            String hashHex = parts[3];
            String computedHash = pbkdf2Sm3(rawPassword.toString(), saltHex, iterations);
            return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), hashHex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private String pbkdf2Sm3(String password, String saltHex, int iterations) {
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] saltBytes = Hex.decode(saltHex);
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SM3Digest());
        generator.init(passwordBytes, saltBytes, iterations);
        byte[] key = ((KeyParameter) generator.generateDerivedParameters(256)).getKey();
        return Hex.toHexString(key);
    }

}
