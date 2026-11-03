package com.humolix.volur.framework.common.thread;

import com.humolix.volur.framework.domain.vo.ThreadMetricsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolurThreadManagerTest {

    private AsyncProperties properties;
    private AsyncContextTaskDecorator taskDecorator;
    private VolurThreadManager threadManager;

    @BeforeEach
    void setUp() {
        properties = new AsyncProperties();
        properties.setVirtualThreadsEnabled(true);
        properties.setMaxConcurrentVirtualTasks(5);
        properties.setPermitAcquireTimeoutMs(100L);
        properties.setTerminationTimeoutSeconds(5);
        properties.setComputeCorePoolSize(2);
        properties.setComputeMaxPoolSize(4);
        properties.setComputeQueueCapacity(50);

        taskDecorator = new AsyncContextTaskDecorator();
        threadManager = new VolurThreadManager(properties, taskDecorator);
        threadManager.init();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        threadManager.destroy();
    }

    @Test
    @DisplayName("验证 runAsync 在 Java 25 虚拟线程上调度执行")
    void testRunAsyncOnVirtualThread() throws Exception {
        AtomicBoolean isVirtual = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<Void> future = threadManager.runAsync(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            latch.countDown();
        });

        future.get(3, TimeUnit.SECONDS);
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(isVirtual.get()).isTrue();
    }

    @Test
    @DisplayName("验证 supplyAsync 在虚拟线程上执行并正常返回结果")
    void testSupplyAsyncOnVirtualThread() throws Exception {
        CompletableFuture<String> future = threadManager.supplyAsync(() -> {
            assertThat(Thread.currentThread().isVirtual()).isTrue();
            return "volur-virtual-result";
        });

        String result = future.get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("volur-virtual-result");
    }

    @Test
    @DisplayName("验证 runCompute 在物理平台线程上调度，避免占用 carrier thread")
    void testRunComputeOnPlatformThread() throws Exception {
        AtomicBoolean isVirtual = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<Void> future = threadManager.runCompute(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            latch.countDown();
        });

        future.get(3, TimeUnit.SECONDS);
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(isVirtual.get()).isFalse();
    }

    @Test
    @DisplayName("验证 MDC 与 SecurityContext 跨虚拟线程自动传递与安全清理")
    void testContextPropagation() throws Exception {
        String testTraceId = "test-trace-123456";
        MDC.put(AsyncContextTaskDecorator.MDC_TRACE_ID_KEY, testTraceId);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "credential", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        SecurityContextHolder.setContext(securityContext);

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        AtomicReference<String> capturedUsername = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        threadManager.runAsync(() -> {
            capturedTraceId.set(MDC.get(AsyncContextTaskDecorator.MDC_TRACE_ID_KEY));
            SecurityContext sc = SecurityContextHolder.getContext();
            if (sc != null && sc.getAuthentication() != null) {
                capturedUsername.set(sc.getAuthentication().getName());
            }
            latch.countDown();
        }).get(3, TimeUnit.SECONDS);

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isEqualTo(testTraceId);
        assertThat(capturedUsername.get()).isEqualTo("admin");
    }

    @Test
    @DisplayName("验证虚拟线程背压限流机制：许可耗尽时抛出 RejectedExecutionException")
    void testBackpressurePermitLimiting() throws Exception {
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(5);

        // 占满全部 5 个许可
        for (int i = 0; i < 5; i++) {
            threadManager.runWithPermit(() -> {
                startedLatch.countDown();
                try {
                    blockLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertThat(startedLatch.await(3, TimeUnit.SECONDS)).isTrue();

        // 尝试获取第 6 个许可，应当超时并被背压拒绝
        assertThatThrownBy(() -> threadManager.runWithPermit(() -> {}))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("背压阈值");

        // 释放阻塞，许可回退
        blockLatch.countDown();

        // 等待许可释放后再次执行应能成功
        boolean executed = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            if (threadManager.tryRunWithPermit(() -> {}, Duration.ofMillis(100))) {
                executed = true;
                break;
            }
        }
        assertThat(executed).isTrue();
    }

    @Test
    @DisplayName("验证 ThreadMetricsVO 指标快照统计准确性")
    void testMetrics() throws Exception {
        CompletableFuture<Void> future = threadManager.runAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        ThreadMetricsVO metrics = threadManager.getMetrics();
        assertThat(metrics.getVirtualThreadsEnabled()).isTrue();
        assertThat(metrics.getMaxConcurrentVirtualTasks()).isEqualTo(5);
        assertThat(metrics.getAvailableProcessors()).isGreaterThan(0);

        future.get(3, TimeUnit.SECONDS);
        Thread.sleep(50);

        ThreadMetricsVO updatedMetrics = threadManager.getMetrics();
        assertThat(updatedMetrics.getCompletedVirtualTasks()).isGreaterThanOrEqualTo(1L);
    }

}
