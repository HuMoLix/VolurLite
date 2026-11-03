package com.humolix.volur.verification.crypto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaCryptoService {

    private final Rc4CaptchaEncoder rc4CaptchaEncoder;

    public CaptchaCryptoService() {
        this.rc4CaptchaEncoder = new Rc4CaptchaEncoder();
    }

    private static final String DEFAULT_RSA_PRIVATE_KEY_PEM =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC74Bc+ZigbNoD4" +
            "XtBu/bFfITv2QFiE6QxPNbk0mU4/CwJCkHurs7zrwaxlNEfc8eCVg6dttKm8mLeu" +
            "y0QeApB32n0bndzjFxmTcQNYIK1O32RTtxGLBiNKi7EQzFShiyBUQrAPDg3mYcWt" +
            "QYxxTgtE+cv+WwklD3W4ENQNveJ3BNGXlria/8Qx0H6krLQLAvZM0Xz7fUIACFvE" +
            "wVa8g1B6bBPlPGhNcVUNWI4NeboXmzgAtCMptjZFjeB+Ub1EgSyPPnA+7aupTQAc" +
            "A5Ut0FDHvWxg2XFebvQ8ANo1sM1Og5I0BQa9CvMYPW0typpxUdsMrLoOR1t3T4VG" +
            "I98COTTNAgMBAAECggEAFIzrPfdtWlIDQtJsOO0lzQ1qQldrggOpRbN6B2zgMuPW" +
            "DXNByS2nSUqCvFc+/RtgFjqzwmZJTXM5tF77m89qqhGLhHfSGkJZQ9gle4BSNMHE" +
            "IabGvnmfZp9nxjnzj/ATSzmNieK0VoefWIJGWyZ5nm6Mh81sGf0E7Wtke/78mfQM" +
            "MrYJbBWntS1cxfGS3rTX2B/mwk04VMOoZzKGNp1z4ECYB1C86ISeJpHhZFzfIbmS" +
            "P7q9i5bskde/ZJdJVU3fXX1PpKF16QBCEKh74rp35RlGfh0zM9QrROFhXAWW4HFE" +
            "/aCV1cAQ035o4UsC5BsIJlEOD0NyI+IWOA9oGrV7KQKBgQDsvuKpJ2NTu8QVw/FB" +
            "Sg75pgHLbClUVhR3sAw1v0k9gBUgDGPtWQGB7BeEGbw63TYKhPIv3eU3VKbV136q" +
            "zauT3bnzAQ93zKmZP4Hm9tYaC0oQBgnt5R/trSXTvLNtyrig9/4NZNrNY/98ebfN" +
            "jRs95aJrb3yTXmqpaKt8jwDI6wKBgQDLJ7YgeDD060j6LMKoGIznySBws5ZN6toQ" +
            "4SfIN/iuhYYzM5h0VtNW1k66vIujg1DSx1utH83AnCMcZlqWBq7EQoDy+IyektRi" +
            "CxoHI65LOdNj5k8Al5cZR9zfqgvpPSzPg955IVDWg0wv/m1zt423OJEpfkn7SHoI" +
            "JDcTQ0mLJwKBgAf1RrM+8rTO3AGLS6HRNn2n2emXojAGkLuRPGfWHUUm+A7Su80v" +
            "r8BJSmUTZGNqKjXiUC3RvLHGc7E+hDCDp1/aFTUY9Qcm5xCStPApnfrUJFIzkcPg" +
            "btlTd7ENZ86qANjQXUbf/AbpdJ4fjVEojCDhhjssTTi69MTrEtCmEfWvAoGAaPbS" +
            "pgIToWezo/beeEV+//8t8K0JOqbeFpyOlmc86Mb6aJf+7BU0Soid0uO57jjZ2J3C" +
            "Yzeschj6FyFXKm63e5W01FTRKtlIPIPp5chstFdjPhlvtimcmyZDG5jbYJ4H+gvz" +
            "NXieMMdT5PQ7Du/Gm+TxM5QZm2h+DJjRj/VTGT8CgYBM+Uun28UjPSQm3v88Pvjv" +
            "iYCpSYlTnGOWZAoxK6mYwJhki1n9+KBo3W5cWvCweTOxe6CiChHFrufIHQlv1qyy" +
            "BG7gV3sGdPCWePBrJxlch67qPTr089t8ab+krpMYkbV5QdnctPqaY/oaXL6jXe1K" +
            "i8Aaj7VGG+6cW8kEQKeWYA==";

    public static final String DEFAULT_RSA_PUBLIC_KEY_PEM =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu+AXPmYoGzaA+F7Qbv2x" +
            "XyE79kBYhOkMTzW5NJlOPwsCQpB7q7O868GsZTRH3PHglYOnbbSpvJi3rstEHgKQ" +
            "d9p9G53c4xcZk3EDWCCtTt9kU7cRiwYjSouxEMxUoYsgVEKwDw4N5mHFrUGMcU4L" +
            "RPnL/lsJJQ91uBDUDb3idwTRl5a4mv/EMdB+pKy0CwL2TNF8+31CAAhbxMFWvINQ" +
            "emwT5TxoTXFVDViODXm6F5s4ALQjKbY2RY3gflG9RIEsjz5wPu2rqU0AHAOVLdBQ" +
            "x71sYNlxXm70PADaNbDNToOSNAUGvQrzGD1tLcqacVHbDKy6Dkdbd0+FRiPfAjk0" +
            "zQIDAQAB";

    @Value("${volur.captcha.crypto.rsa-private-key:${volur.security.captcha.rsa-private-key:}}")
    private String customRsaPrivateKey;

    @Value("${volur.captcha.crypto.rsa-public-key:}")
    private String customRsaPublicKey;

    @Value("${volur.captcha.crypto.anti-replay-window-ms:120000}")
    private long antiReplayWindowMs;

    private PrivateKey privateKey;
    private final ObjectMapper objectMapper = new JsonMapper();

    /**
     * 获取用于前端加密场景的 RSA 公钥 (PEM 纯 Base64 内容)
     */
    public String getPublicKeyPem() {
        if (StringUtils.hasText(customRsaPublicKey)) {
            return customRsaPublicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
        }
        return DEFAULT_RSA_PUBLIC_KEY_PEM;
    }

    @PostConstruct
    public void init() {
        try {
            String pem = StringUtils.hasText(customRsaPrivateKey) ? customRsaPrivateKey : DEFAULT_RSA_PRIVATE_KEY_PEM;
            String cleanKey = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(keySpec);
            log.info("[Volur-Verification] CaptchaCryptoService 成功加载场景混合加解密私钥");
        } catch (Exception ex) {
            log.error("[Volur-Verification] 初始化 RSA 验证码私钥失败", ex);
            throw new IllegalStateException("Failed to initialize Captcha RSA Private Key", ex);
        }
    }

    /**
     * 解密前端提交的 captchaSceneId 载荷
     *
     * @param captchaSceneId 格式: ${encSessionKeyBase64}.${rc4CiphertextBase64}
     * @return 解密并经校验合法的路由路径 (如 "/auth/login")
     */
    public String decryptScenePath(String captchaSceneId) {
        if (!StringUtils.hasText(captchaSceneId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "场景验证凭据不能为空");
        }

        String[] parts = captchaSceneId.trim().split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "场景验证凭据结构非法");
        }

        try {
            // 1. RSA 解密会话密钥
            byte[] encKeyBytes = Base64.getDecoder().decode(parts[0]);
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            byte[] sessionKey = rsaCipher.doFinal(encKeyBytes);

            // 2. 魔改 RC4 解密载荷明文
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
            byte[] plainBytes = rc4CaptchaEncoder.decode(cipherBytes, sessionKey);
            String plainJson = new String(plainBytes, StandardCharsets.UTF_8).trim();

            // 3. 解析 JSON 载荷
            String path;
            long timestamp;

            if (plainJson.startsWith("{") && plainJson.endsWith("}")) {
                JsonNode node = objectMapper.readTree(plainJson);
                path = node.has("path") ? node.get("path").asText() : "";
                timestamp = node.has("ts") ? node.get("ts").asLong() : 0L;
            } else {
                // 回退兼容直接传路径字符串的场景
                path = plainJson;
                timestamp = System.currentTimeMillis();
            }

            if (!StringUtils.hasText(path)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "场景凭据中未包含有效请求源路由");
            }

            // 4. 防重放校验：请求时间窗口校验
            long current = System.currentTimeMillis();
            long limit = this.antiReplayWindowMs > 0 ? this.antiReplayWindowMs : 120_000L;
            if (timestamp > 0 && Math.abs(current - timestamp) > limit) {
                log.warn("[Volur-Verification] 验证码场景时间戳已失效: clientTs={}, serverTs={}, drift={}ms, threshold={}ms",
                        timestamp, current, Math.abs(current - timestamp), limit);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码请求安全凭据已失效，请刷新重试");
            }

            return path.trim().toLowerCase();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.warn("[Volur-Verification] 解密验证码场景凭据失败: {}", ex.getMessage());
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人机安全场景凭据解密失败");
        }
    }

    public String decryptPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "加密负载数据不能为空");
        }
        String clean = payload.trim();
        if (clean.contains(".")) {
            String[] parts = clean.split("\\.");
            try {
                byte[] encKeyBytes = Base64.getDecoder().decode(parts[0]);
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
                byte[] sessionKey = rsaCipher.doFinal(encKeyBytes);

                byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
                byte[] plainBytes = rc4CaptchaEncoder.decode(cipherBytes, sessionKey);
                return new String(plainBytes, StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                log.warn("[CaptchaCryptoService] 混合解密失败: {}", e.getMessage());
                throw new BusinessException(HttpStatus.BAD_REQUEST, "加密负载解密失败");
            }
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(clean);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            int blockSize = 256;
            if (cipherBytes.length <= blockSize) {
                byte[] decrypted = cipher.doFinal(cipherBytes);
                return new String(decrypted, StandardCharsets.UTF_8).trim();
            } else {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                for (int offset = 0; offset < cipherBytes.length; offset += blockSize) {
                    int inputLen = Math.min(blockSize, cipherBytes.length - offset);
                    byte[] block = cipher.doFinal(cipherBytes, offset, inputLen);
                    out.write(block);
                }
                return out.toString(StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ex) {
            log.warn("[CaptchaCryptoService] RSA 解密失败: {}", ex.getMessage());
            throw new BusinessException(HttpStatus.BAD_REQUEST, "加密数据解析失败");
        }
    }

}

