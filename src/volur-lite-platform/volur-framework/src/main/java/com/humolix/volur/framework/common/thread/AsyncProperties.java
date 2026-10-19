package com.humolix.volur.framework.common.thread;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "volur.async")
public class AsyncProperties {

    /**
     * 是否启用 Java 25 虚拟线程
     */
    private boolean virtualThreadsEnabled = true;

    /**
     * 虚拟线程名称前缀
     */
    private String threadNamePrefix = "volur-vt-";

    /**
     * 定时调度虚拟线程名称前缀
     */
    private String scheduleThreadNamePrefix = "volur-sched-vt-";

    /**
     * 虚拟线程并发保护信号量阈值（防止突发海量 I/O 请求击垮下游数据库连接池或第三方服务）
     */
    private int maxConcurrentVirtualTasks = 10_000;

    /**
     * 信号量获取超时时间（毫秒）
     */
    private long permitAcquireTimeoutMs = 5_000L;

    /**
     * 优雅停机等待超时时间（秒）
     */
    private int terminationTimeoutSeconds = 30;

    /**
     * CPU 密集型计算线程池核心线程数（默认等于当前机器可用 CPU 逻辑核心数）
     */
    private int computeCorePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * CPU 密集型计算线程池最大线程数
     */
    private int computeMaxPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);

    /**
     * CPU 密集型计算线程池工作队列容量
     */
    private int computeQueueCapacity = 1_000;

    /**
     * CPU 密集型计算线程名称前缀
     */
    private String computeThreadNamePrefix = "volur-cpu-";

}
