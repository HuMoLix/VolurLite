package com.humolix.volur.security.crypto;

import com.humolix.volur.framework.common.thread.AsyncContextTaskDecorator;
import com.humolix.volur.framework.common.thread.AsyncProperties;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sm3PasswordEncoderTest {

    private AsyncProperties properties;
    private AsyncContextTaskDecorator taskDecorator;
    private VolurThreadManager threadManager;
    private Sm3PasswordEncoder asyncPasswordEncoder;
    private Sm3PasswordEncoder syncFallbackEncoder;

    @BeforeEach
    void setUp() {
        properties = new AsyncProperties();
        properties.setVirtualThreadsEnabled(true);
        properties.setComputeCorePoolSize(2);
        properties.setComputeMaxPoolSize(4);
        properties.setComputeQueueCapacity(50);
        properties.setComputeThreadNamePrefix("volur-cpu-test-");

        taskDecorator = new AsyncContextTaskDecorator();
        threadManager = new VolurThreadManager(properties, taskDecorator);
        threadManager.init();

        asyncPasswordEncoder = new Sm3PasswordEncoder(threadManager);
        syncFallbackEncoder = new Sm3PasswordEncoder();
    }

    @AfterEach
    void tearDown() {
        if (threadManager != null) {
            threadManager.destroy();
        }
    }

    @Test
    @DisplayName("验证密码哈希生成与校验正确性（基于 VolurThreadManager 异步计算调度）")
    void testEncodeAndMatchesWithThreadManager() {
        String rawPassword = "SecurePassword2026!@#";
        String encoded = asyncPasswordEncoder.encode(rawPassword);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).startsWith("pbkdf2_sm3$10000$");

        assertThat(asyncPasswordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(asyncPasswordEncoder.matches("WrongPassword!@#", encoded)).isFalse();
    }

    @Test
    @DisplayName("验证 encodeAsync 与 matchesAsync 异步非阻塞执行管道")
    void testEncodeAsyncAndMatchesAsync() throws Exception {
        String rawPassword = "AsyncPipelinePassword999$";

        CompletableFuture<String> encodeFuture = asyncPasswordEncoder.encodeAsync(rawPassword);
        String encoded = encodeFuture.get(5, TimeUnit.SECONDS);

        assertThat(encoded).startsWith("pbkdf2_sm3$10000$");

        CompletableFuture<Boolean> matchSuccessFuture = asyncPasswordEncoder.matchesAsync(rawPassword, encoded);
        CompletableFuture<Boolean> matchFailFuture = asyncPasswordEncoder.matchesAsync("BadPass!", encoded);

        assertThat(matchSuccessFuture.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(matchFailFuture.get(5, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    @DisplayName("验证无线程管理器时回退至本地同步执行")
    void testFallbackWithoutThreadManager() {
        String rawPassword = "FallbackPassword123#";
        String encoded = syncFallbackEncoder.encode(rawPassword);

        assertThat(encoded).startsWith("pbkdf2_sm3$10000$");
        assertThat(syncFallbackEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(syncFallbackEncoder.matches("Wrong!", encoded)).isFalse();
    }

    @Test
    @DisplayName("验证空密码及非法入参防御")
    void testInvalidPasswordInput() {
        assertThatThrownBy(() -> asyncPasswordEncoder.encode(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(asyncPasswordEncoder.matches("", "pbkdf2_sm3$10000$salt$hash")).isFalse();
        assertThat(asyncPasswordEncoder.matches("password", "invalid_format")).isFalse();
    }

}
