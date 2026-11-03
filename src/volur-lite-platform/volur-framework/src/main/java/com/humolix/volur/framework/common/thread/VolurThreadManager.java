package com.humolix.volur.framework.common.thread;

import com.humolix.volur.framework.domain.vo.ThreadMetricsVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolurThreadManager implements DisposableBean {

    private final AsyncProperties asyncProperties;
    private final AsyncContextTaskDecorator taskDecorator;

    @Getter
    private ExecutorService virtualExecutor;
    @Getter
    private ThreadPoolExecutor computeExecutor;
    private Semaphore virtualConcurrencyLimiter;

    private final AtomicLong activeVirtualTasks = new AtomicLong(0);
    private final LongAdder completedVirtualTasks = new LongAdder();
    private final LongAdder rejectedVirtualTasks = new LongAdder();

    @PostConstruct
    public void init() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name(asyncProperties.getThreadNamePrefix(), 0)
                .factory();
        this.virtualExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);

        ThreadFactory computeThreadFactory = Thread.ofPlatform()
                .name(asyncProperties.getComputeThreadNamePrefix(), 0)
                .factory();
        this.computeExecutor = new ThreadPoolExecutor(
                asyncProperties.getComputeCorePoolSize(),
                asyncProperties.getComputeMaxPoolSize(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(asyncProperties.getComputeQueueCapacity()),
                computeThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.virtualConcurrencyLimiter = new Semaphore(asyncProperties.getMaxConcurrentVirtualTasks());

        log.info("[VolurThreadManager] 高并发线程管理器初始化成功: 虚拟线程启用={}, 虚拟任务并发保护上限={}, CPU计算池核心线程数={}, CPU计算池最大线程数={}",
                asyncProperties.isVirtualThreadsEnabled(),
                asyncProperties.getMaxConcurrentVirtualTasks(),
                asyncProperties.getComputeCorePoolSize(),
                asyncProperties.getComputeMaxPoolSize()
        );
    }

    /**
     * 在 Java 25 虚拟线程上执行无返回值异步任务（具备全上下文自动传播与生命周期计数统计）
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        Runnable decorated = taskDecorator.decorate(wrapVirtualTask(task));
        return CompletableFuture.runAsync(decorated, virtualExecutor);
    }

    /**
     * 在 Java 25 虚拟线程上执行有返回值异步任务（具备全上下文自动传播与生命周期计数统计）
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        Supplier<T> decorated = decorateSupplier(wrapVirtualSupplier(supplier));
        return CompletableFuture.supplyAsync(decorated, virtualExecutor);
    }

    /**
     * 基于信号量背压保护的虚拟线程异步任务执行
     * 适用于外部资源紧缺（如数据库连接池、外部 HTTP 接口）的高并发突发流量场景
     */
    public CompletableFuture<Void> runWithPermit(Runnable task) {
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        long timeoutMs = asyncProperties.getPermitAcquireTimeoutMs();
        try {
            boolean acquired = timeoutMs > 0
                    ? virtualConcurrencyLimiter.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
                    : virtualConcurrencyLimiter.tryAcquire();
            if (!acquired) {
                rejectedVirtualTasks.increment();
                log.warn("[VolurThreadManager] 虚拟线程并发保护触发背压限流: 无法在 {}ms 内获取执行许可", timeoutMs);
                throw new RejectedExecutionException("高并发任务达到背压阈值，无法获取执行许可");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rejectedVirtualTasks.increment();
            throw new RejectedExecutionException("获取虚拟线程执行许可时线程被中断", e);
        }

        Runnable wrappedTask = () -> {
            try {
                task.run();
            } finally {
                virtualConcurrencyLimiter.release();
            }
        };

        return runAsync(wrappedTask);
    }

    /**
     * 基于信号量背压保护的虚拟线程有返回值异步任务执行
     */
    public <T> CompletableFuture<T> supplyWithPermit(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        long timeoutMs = asyncProperties.getPermitAcquireTimeoutMs();
        try {
            boolean acquired = timeoutMs > 0
                    ? virtualConcurrencyLimiter.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
                    : virtualConcurrencyLimiter.tryAcquire();
            if (!acquired) {
                rejectedVirtualTasks.increment();
                log.warn("[VolurThreadManager] 虚拟线程并发保护触发背压限流: 无法在 {}ms 内获取执行许可", timeoutMs);
                throw new RejectedExecutionException("高并发任务达到背压阈值，无法获取执行许可");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rejectedVirtualTasks.increment();
            throw new RejectedExecutionException("获取虚拟线程执行许可时线程被中断", e);
        }

        Supplier<T> wrappedSupplier = () -> {
            try {
                return supplier.get();
            } finally {
                virtualConcurrencyLimiter.release();
            }
        };

        return supplyAsync(wrappedSupplier);
    }

    /**
     * 尝试在指定时间内获取许可并执行任务，若超时未获取许可则安全放弃并返回 false
     */
    public boolean tryRunWithPermit(Runnable task, Duration timeout) {
        if (task == null) {
            return false;
        }
        try {
            boolean acquired = virtualConcurrencyLimiter.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                rejectedVirtualTasks.increment();
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rejectedVirtualTasks.increment();
            return false;
        }

        runAsync(() -> {
            try {
                task.run();
            } finally {
                virtualConcurrencyLimiter.release();
            }
        });
        return true;
    }

    /**
     * 提交 CPU 密集型任务至平台计算线程池，避免 carrier thread 被持续占用导致其他虚拟线程饥饿
     */
    public CompletableFuture<Void> runCompute(Runnable task) {
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        Runnable decorated = taskDecorator.decorate(task);
        return CompletableFuture.runAsync(decorated, computeExecutor);
    }

    /**
     * 提交 CPU 密集型有返回值任务至平台计算线程池
     */
    public <T> CompletableFuture<T> supplyCompute(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        Supplier<T> decorated = decorateSupplier(supplier);
        return CompletableFuture.supplyAsync(decorated, computeExecutor);
    }

    /**
     * 获取当前线程管理器的健康指标与并发快照
     */
    public ThreadMetricsVO getMetrics() {
        return ThreadMetricsVO.builder()
                .virtualThreadsEnabled(asyncProperties.isVirtualThreadsEnabled())
                .activeVirtualTasks(activeVirtualTasks.get())
                .completedVirtualTasks(completedVirtualTasks.sum())
                .rejectedVirtualTasks(rejectedVirtualTasks.sum())
                .availableIoPermits(virtualConcurrencyLimiter.availablePermits())
                .maxConcurrentVirtualTasks(asyncProperties.getMaxConcurrentVirtualTasks())
                .computeCorePoolSize(computeExecutor.getCorePoolSize())
                .computeMaxPoolSize(computeExecutor.getMaximumPoolSize())
                .computeActiveCount(computeExecutor.getActiveCount())
                .computeQueueSize(computeExecutor.getQueue().size())
                .computeCompletedTaskCount(computeExecutor.getCompletedTaskCount())
                .availableProcessors(Runtime.getRuntime().availableProcessors())
                .build();
    }

    private Runnable wrapVirtualTask(Runnable task) {
        return () -> {
            activeVirtualTasks.incrementAndGet();
            try {
                task.run();
            } finally {
                activeVirtualTasks.decrementAndGet();
                completedVirtualTasks.increment();
            }
        };
    }

    private <T> Supplier<T> wrapVirtualSupplier(Supplier<T> supplier) {
        return () -> {
            activeVirtualTasks.incrementAndGet();
            try {
                return supplier.get();
            } finally {
                activeVirtualTasks.decrementAndGet();
                completedVirtualTasks.increment();
            }
        };
    }

    private <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return () -> {
            final Object[] resultHolder = new Object[1];
            final Throwable[] errorHolder = new Throwable[1];
            Runnable task = () -> {
                try {
                    resultHolder[0] = supplier.get();
                } catch (Throwable t) {
                    errorHolder[0] = t;
                }
            };
            taskDecorator.decorate(task).run();
            if (errorHolder[0] != null) {
                if (errorHolder[0] instanceof RuntimeException re) {
                    throw re;
                }
                throw new CompletionException(errorHolder[0]);
            }
            @SuppressWarnings("unchecked")
            T val = (T) resultHolder[0];
            return val;
        };
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("[VolurThreadManager] 正在启动优雅停机流程，等待所有在途异步任务执行完成...");
        int timeoutSeconds = asyncProperties.getTerminationTimeoutSeconds();

        if (virtualExecutor != null) {
            virtualExecutor.shutdown();
        }
        if (computeExecutor != null) {
            computeExecutor.shutdown();
        }

        try {
            boolean virtualDone = virtualExecutor == null || virtualExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
            boolean computeDone = computeExecutor == null || computeExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);

            if (!virtualDone) {
                log.warn("[VolurThreadManager] 虚拟线程池停机超时 ({}s)，强制终止剩余在途任务", timeoutSeconds);
                virtualExecutor.shutdownNow();
            }
            if (!computeDone) {
                log.warn("[VolurThreadManager] CPU计算线程池停机超时 ({}s)，强制终止剩余在途任务", timeoutSeconds);
                computeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[VolurThreadManager] 停机等待被中断，强制终止所有线程池");
            if (virtualExecutor != null) {
                virtualExecutor.shutdownNow();
            }
            if (computeExecutor != null) {
                computeExecutor.shutdownNow();
            }
        }
        log.info("[VolurThreadManager] 线程管理器已安全注销关闭完成");
    }

}
