package com.humolix.volur.verification;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.crypto.Rc4CaptchaEncoder;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import com.humolix.volur.verification.risk.enums.CaptchaType;
import com.humolix.volur.verification.risk.policy.CaptchaRoutePolicyManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CaptchaCryptoServiceTest {

    private CaptchaCryptoService cryptoService;
    private CaptchaRoutePolicyManager routePolicyManager;
    private PublicKey rsaPublicKey;

    @BeforeEach
    public void setUp() throws Exception {
        cryptoService = new CaptchaCryptoService(new Rc4CaptchaEncoder());
        cryptoService.init();
        routePolicyManager = new CaptchaRoutePolicyManager();

        byte[] pubBytes = Base64.getDecoder().decode(CaptchaCryptoService.DEFAULT_RSA_PUBLIC_KEY_PEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        rsaPublicKey = keyFactory.generatePublic(keySpec);
    }

    private String encryptClientScene(String path, long timestamp) throws Exception {
        byte[] sessionKey = "1234567890abcdef".getBytes(StandardCharsets.UTF_8);

        // 1. RSA 加密 sessionKey
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encKeyBytes = rsaCipher.doFinal(sessionKey);
        String encKeyBase64 = Base64.getEncoder().encodeToString(encKeyBytes);

        // 2. 魔改 RC4 加密 JSON
        String json = "{\"path\":\"" + path + "\",\"ts\":" + timestamp + "}";
        Rc4CaptchaEncoder rc4Encoder = new Rc4CaptchaEncoder();
        byte[] cipherBytes = rc4Encoder.encode(json.getBytes(StandardCharsets.UTF_8), sessionKey);
        String cipherBase64 = Base64.getEncoder().encodeToString(cipherBytes);

        return encKeyBase64 + "." + cipherBase64;
    }

    @Test
    public void testDecryptLoginScenePathSuccessfully() throws Exception {
        long now = System.currentTimeMillis();
        String sceneId = encryptClientScene("/auth/login", now);

        String path = cryptoService.decryptScenePath(sceneId);
        Assertions.assertEquals("/auth/login", path);

        CaptchaRoutePolicyManager.RoutePolicy policy = routePolicyManager.resolvePolicy(path);
        Assertions.assertEquals(CaptchaScene.LOGIN, policy.getScene());
        Assertions.assertEquals(CaptchaType.LINE, policy.getBaseType());
    }

    @Test
    public void testDecryptRegisterSceneResolvesSlider() throws Exception {
        long now = System.currentTimeMillis();
        String sceneId = encryptClientScene("/auth/register", now);

        String path = cryptoService.decryptScenePath(sceneId);
        Assertions.assertEquals("/auth/register", path);

        CaptchaRoutePolicyManager.RoutePolicy policy = routePolicyManager.resolvePolicy(path);
        Assertions.assertEquals(CaptchaScene.REGISTER, policy.getScene());
        Assertions.assertEquals(CaptchaType.SLIDE_PUZZLE, policy.getBaseType());
    }

    @Test
    public void testReplayAttackExpiredTimestampThrowsException() throws Exception {
        // 超过 120 秒前的时间戳 (3 分钟前)
        long expiredTimestamp = System.currentTimeMillis() - 180_000L;
        String sceneId = encryptClientScene("/auth/login", expiredTimestamp);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () ->
                cryptoService.decryptScenePath(sceneId)
        );
        Assertions.assertTrue(ex.getMessage().contains("失效") || ex.getMessage().contains("过期"));
    }

    @Test
    public void testCorruptedCiphertextThrowsException() {
        Assertions.assertThrows(BusinessException.class, () ->
                cryptoService.decryptScenePath("invalid.ciphertext")
        );
    }

    @Test
    public void testGetPublicKeyPemReturnsValidBase64Key() {
        String publicKey = cryptoService.getPublicKeyPem();
        Assertions.assertNotNull(publicKey);
        Assertions.assertFalse(publicKey.isEmpty());
        Assertions.assertFalse(publicKey.contains("BEGIN PUBLIC KEY"));
        Assertions.assertDoesNotThrow(() -> Base64.getDecoder().decode(publicKey));
    }

}

