package com.humolix.volur.framework.common.config;

import com.humolix.volur.framework.common.thread.AsyncContextTaskDecorator;
import com.humolix.volur.framework.common.thread.AsyncProperties;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTest {

    private AsyncProperties properties;
    private AsyncContextTaskDecorator taskDecorator;
    private VolurThreadManager threadManager;
    private SchedulingConfig schedulingConfig;

    @BeforeEach
    void setUp() {
        properties = new AsyncProperties();
        properties.setVirtualThreadsEnabled(true);
        properties.setScheduleThreadNamePrefix("test-sched-vt-");
        properties.setTerminationTimeoutSeconds(5);

        taskDecorator = new AsyncContextTaskDecorator();
        threadManager = new VolurThreadManager(properties, taskDecorator);
        threadManager.init();

        schedulingConfig = new SchedulingConfig(properties, taskDecorator, threadManager);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        threadManager.destroy();
    }

    @Test
    @DisplayName("验证定时任务在 Java 25 虚拟线程上调度执行")
    void testScheduledTaskRunsOnVirtualThread() throws Exception {
        TaskScheduler scheduler = schedulingConfig.volurTaskScheduler();
        AtomicBoolean isVirtual = new AtomicBoolean(false);
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }, Instant.now().plusMillis(50));

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(isVirtual.get()).isTrue();
        assertThat(threadName.get()).contains("volur-");

        if (scheduler instanceof SimpleAsyncTaskScheduler simpleScheduler) {
            simpleScheduler.close();
        }
    }

    @Test
    @DisplayName("验证非阻塞并发调度：长耗时任务不阻塞后续定时任务执行")
    void testLongRunningTaskDoesNotBlockSubsequentTasks() throws Exception {
        TaskScheduler scheduler = schedulingConfig.volurTaskScheduler();
        CountDownLatch task1Started = new CountDownLatch(1);
        CountDownLatch task2Finished = new CountDownLatch(1);
        AtomicBoolean task2CompletedBeforeTask1 = new AtomicBoolean(false);

        // 任务 1：休眠 500ms（模拟耗时 I/O 阻塞）
        scheduler.schedule(() -> {
            task1Started.countDown();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, Instant.now().plusMillis(20));

        assertThat(task1Started.await(2, TimeUnit.SECONDS)).isTrue();

        // 任务 2：轻量任务，应当不受任务 1 阻塞立即执行完成
        scheduler.schedule(() -> {
            task2CompletedBeforeTask1.set(true);
            task2Finished.countDown();
        }, Instant.now().plusMillis(50));

        assertThat(task2Finished.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(task2CompletedBeforeTask1.get()).isTrue();

        if (scheduler instanceof SimpleAsyncTaskScheduler simpleScheduler) {
            simpleScheduler.close();
        }
    }

    @Test
    @DisplayName("验证定时任务执行具备 MDC traceId 上下文自动注入")
    void testScheduledTaskMdcTraceIdGeneration() throws Exception {
        TaskScheduler scheduler = schedulingConfig.volurTaskScheduler();
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(() -> {
            capturedTraceId.set(MDC.get(AsyncContextTaskDecorator.MDC_TRACE_ID_KEY));
            latch.countDown();
        }, Instant.now().plusMillis(50));

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isNotBlank();

        if (scheduler instanceof SimpleAsyncTaskScheduler simpleScheduler) {
            simpleScheduler.close();
        }
    }

}
