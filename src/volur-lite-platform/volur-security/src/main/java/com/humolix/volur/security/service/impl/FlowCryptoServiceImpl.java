package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.service.FlowCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class FlowCryptoServiceImpl implements FlowCryptoService {

    private static final String STATIC_SECRET = "VolurSecret2026@139284#$$";
    private static final String ENVELOPE_PREFIX = "$V1$GCM$";
    private static final long MAX_SKEW_MS = 300_000L;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Override
    public String decrypt(String envelope, String flowName, String flowEntry, String dsh) {
        if (!StringUtils.hasText(envelope)) {
            return envelope;
        }
        if (!envelope.startsWith(ENVELOPE_PREFIX)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "凭据未加密或安全信封格式非法");
        }
        String content = envelope.substring(ENVELOPE_PREFIX.length());
        int splitIdx = content.indexOf('$');
        if (splitIdx <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "安全信封格式损坏");
        }
        String tsStr = content.substring(0, splitIdx);
        String cipherBase64 = content.substring(splitIdx + 1);

        long timestamp;
        try {
            timestamp = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "安全信封时间戳非法");
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > MAX_SKEW_MS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求凭证已过期或存在重放风险");
        }

        byte[] payloadBytes;
        try {
            String sanitized = cipherBase64.trim().replace(' ', '+');
            payloadBytes = Base64.getDecoder().decode(sanitized);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密文数据Base64解码失败");
        }

        if (payloadBytes.length <= GCM_IV_LENGTH + 16) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密文数据长度不合法");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(payloadBytes, 0, iv, 0, GCM_IV_LENGTH);

        int cipherLen = payloadBytes.length - GCM_IV_LENGTH;
        byte[] cipherAndTag = new byte[cipherLen];
        System.arraycopy(payloadBytes, GCM_IV_LENGTH, cipherAndTag, 0, cipherLen);

        SecretKeySpec keySpec = deriveKey(flowName, flowEntry, dsh, timestamp);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            byte[] plainBytes = cipher.doFinal(cipherAndTag);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "凭证校验失败，数据被篡改或参数不匹配");
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "凭证解密异常: " + e.getMessage());
        }
    }

    @Override
    public String encrypt(String plainText, String flowName, String flowEntry, String dsh) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        long timestamp = System.currentTimeMillis();
        SecretKeySpec keySpec = deriveKey(flowName, flowEntry, dsh, timestamp);

        byte[] iv = new byte[GCM_IV_LENGTH];
        ThreadLocalRandom.current().nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, payload, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, payload, GCM_IV_LENGTH, cipherBytes.length);

            String base64Payload = Base64.getEncoder().encodeToString(payload);
            return ENVELOPE_PREFIX + timestamp + "$" + base64Payload;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "凭证加密异常: " + e.getMessage());
        }
    }

    private SecretKeySpec deriveKey(String flowName, String flowEntry, String dsh, long timestamp) {
        String dynamicStr = (flowName != null ? flowName : "") + ":" +
                (flowEntry != null ? flowEntry : "") + ":" +
                (dsh != null ? dsh : "") + ":" +
                timestamp;
        String dynamicHex = DigestUtils.md5DigestAsHex(dynamicStr.getBytes(StandardCharsets.UTF_8));
        String rawKey = STATIC_SECRET + "#" + dynamicHex;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
