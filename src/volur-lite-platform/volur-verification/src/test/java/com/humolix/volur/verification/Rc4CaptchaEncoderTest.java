package com.humolix.volur.verification;

import com.humolix.volur.framework.common.thread.AsyncContextTaskDecorator;
import com.humolix.volur.framework.common.thread.AsyncProperties;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.verification.crypto.Rc4CaptchaEncoder;
import com.humolix.volur.verification.crypto.Rc4Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rc4CaptchaEncoderTest {

    private AsyncProperties properties;
    private AsyncContextTaskDecorator taskDecorator;
    private VolurThreadManager threadManager;
    private Rc4CaptchaEncoder asyncEncoder;
    private Rc4CaptchaEncoder syncFallbackEncoder;

    private static final byte[] TEST_KEY = "TestSecretKey2026".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        properties = new AsyncProperties();
        properties.setVirtualThreadsEnabled(true);
        properties.setComputeCorePoolSize(2);
        properties.setComputeMaxPoolSize(4);
        properties.setComputeQueueCapacity(50);
        properties.setComputeThreadNamePrefix("volur-rc4-test-");

        taskDecorator = new AsyncContextTaskDecorator();
        threadManager = new VolurThreadManager(properties, taskDecorator);
        threadManager.init();

        asyncEncoder = new Rc4CaptchaEncoder(threadManager);
        syncFallbackEncoder = new Rc4CaptchaEncoder();
    }

    @AfterEach
    void tearDown() {
        if (threadManager != null) {
            threadManager.destroy();
        }
    }

    @Test
    @DisplayName("验证魔改 RC4 加密与解密对称性（基于 VolurThreadManager 调度）")
    void testEncodeAndDecodeWithThreadManager() {
        String originalPayload = "{\"path\":\"/auth/login\",\"ts\":1790267157894}";
        byte[] plainBytes = originalPayload.getBytes(StandardCharsets.UTF_8);

        byte[] cipherBytes = asyncEncoder.encode(plainBytes, TEST_KEY);
        assertThat(cipherBytes).isNotNull();
        assertThat(cipherBytes).isNotEqualTo(plainBytes);

        byte[] decryptedBytes = asyncEncoder.decode(cipherBytes, TEST_KEY);
        assertThat(decryptedBytes).isEqualTo(plainBytes);

        String decryptedStr = asyncEncoder.decodeToString(cipherBytes, TEST_KEY);
        assertThat(decryptedStr).isEqualTo(originalPayload);
    }

    @Test
    @DisplayName("验证 encodeAsync 与 decodeAsync 异步非阻塞执行管道")
    void testEncodeAsyncAndDecodeAsync() throws Exception {
        String originalText = "{\"scene\":\"SLIDE_PUZZLE\",\"riskScore\":0}";
        byte[] plainBytes = originalText.getBytes(StandardCharsets.UTF_8);

        CompletableFuture<byte[]> cipherFuture = asyncEncoder.encodeAsync(plainBytes, TEST_KEY);
        byte[] cipherBytes = cipherFuture.get(5, TimeUnit.SECONDS);
        assertThat(cipherBytes).isNotNull();

        CompletableFuture<String> decryptedFuture = asyncEncoder.decodeToStringAsync(cipherBytes, TEST_KEY);
        String decrypted = decryptedFuture.get(5, TimeUnit.SECONDS);
        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test
    @DisplayName("验证无线程管理器时回退至本地同步执行")
    void testFallbackWithoutThreadManager() {
        String rawText = "fallback-rc4-data-payload";
        byte[] plainBytes = rawText.getBytes(StandardCharsets.UTF_8);

        byte[] cipherBytes = syncFallbackEncoder.encode(plainBytes, TEST_KEY);
        assertThat(cipherBytes).isNotNull();

        String decrypted = syncFallbackEncoder.decodeToString(cipherBytes, TEST_KEY);
        assertThat(decrypted).isEqualTo(rawText);
    }

    @Test
    @DisplayName("验证空密钥及非法入参防御")
    void testInvalidKeyAndInput() {
        assertThatThrownBy(() -> asyncEncoder.encode("data".getBytes(StandardCharsets.UTF_8), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asyncEncoder.encode("data".getBytes(StandardCharsets.UTF_8), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);

        byte[] emptyResult = asyncEncoder.encode((byte[]) null, TEST_KEY);
        assertThat(emptyResult).isEmpty();

        byte[] emptyStrResult = asyncEncoder.encode((String) null, TEST_KEY);
        assertThat(emptyStrResult).isEmpty();
    }

    @Test
    @DisplayName("验证 Rc4Util 静态工具类委托的一致性")
    void testRc4UtilDelegation() {
        String raw = "{\"path\":\"/auth/register\"}";
        byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);

        byte[] cipherByUtil = Rc4Util.encrypt(rawBytes, TEST_KEY);
        byte[] cipherByEncoder = asyncEncoder.encode(rawBytes, TEST_KEY);

        assertThat(cipherByUtil).isEqualTo(cipherByEncoder);

        String decryptedByUtil = Rc4Util.decryptToString(cipherByUtil, TEST_KEY);
        assertThat(decryptedByUtil).isEqualTo(raw);
    }

}
