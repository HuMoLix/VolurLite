package com.humolix.volur.security.crypto;

import com.humolix.volur.framework.common.thread.VolurThreadManager;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class Sm3PasswordEncoder implements PasswordEncoder {

    private final VolurThreadManager threadManager;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final String PREFIX = "pbkdf2_sm3";

    public Sm3PasswordEncoder() {
        this.threadManager = null;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        String rawPwd = Objects.requireNonNull(rawPassword).toString();
        if (!StringUtils.hasText(rawPwd)) {
            throw new IllegalArgumentException("The raw password must not be empty");
        }
        byte[] saltBytes = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltHex = Hex.toHexString(saltBytes);
        String hashHex = computeHash(rawPwd, saltHex);
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
            if (rawPassword != null) {
                return verifyHash(rawPassword.toString(), saltHex, iterations, hashHex);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public CompletableFuture<String> encodeAsync(CharSequence rawPassword) {
        String rawPwd = Objects.requireNonNull(rawPassword).toString();
        if (!StringUtils.hasText(rawPwd)) {
            throw new IllegalArgumentException("The raw password must not be empty");
        }
        byte[] saltBytes = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltHex = Hex.toHexString(saltBytes);
        if (threadManager != null) {
            return threadManager.supplyCompute(() -> {
                String hashHex = pbkdf2Sm3(rawPwd, saltHex, ITERATIONS);
                return PREFIX + "$" + ITERATIONS + "$" + saltHex + "$" + hashHex;
            });
        }
        return CompletableFuture.supplyAsync(() -> {
            String hashHex = pbkdf2Sm3(rawPwd, saltHex, ITERATIONS);
            return PREFIX + "$" + ITERATIONS + "$" + saltHex + "$" + hashHex;
        });
    }

    public CompletableFuture<Boolean> matchesAsync(CharSequence rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword) || !encodedPassword.startsWith(PREFIX + "$")) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4) {
                return CompletableFuture.completedFuture(false);
            }
            int iterations = Integer.parseInt(parts[1]);
            String saltHex = parts[2];
            String hashHex = parts[3];
            if (threadManager != null) {
                return threadManager.supplyCompute(() -> {
                    String computedHash = pbkdf2Sm3(rawPassword.toString(), saltHex, iterations);
                    return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), hashHex.getBytes(StandardCharsets.UTF_8));
                });
            }
            return CompletableFuture.supplyAsync(() -> {
                String computedHash = pbkdf2Sm3(rawPassword.toString(), saltHex, iterations);
                return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), hashHex.getBytes(StandardCharsets.UTF_8));
            });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    private String computeHash(String password, String saltHex) {
        if (threadManager != null) {
            return threadManager.supplyCompute(() -> pbkdf2Sm3(password, saltHex, Sm3PasswordEncoder.ITERATIONS)).join();
        }
        return pbkdf2Sm3(password, saltHex, Sm3PasswordEncoder.ITERATIONS);
    }

    private boolean verifyHash(String rawPassword, String saltHex, int iterations, String expectedHashHex) {
        if (threadManager != null) {
            return Boolean.TRUE.equals(threadManager.supplyCompute(() -> {
                String computedHash = pbkdf2Sm3(rawPassword, saltHex, iterations);
                return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), expectedHashHex.getBytes(StandardCharsets.UTF_8));
            }).join());
        }
        String computedHash = pbkdf2Sm3(rawPassword, saltHex, iterations);
        return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), expectedHashHex.getBytes(StandardCharsets.UTF_8));
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
