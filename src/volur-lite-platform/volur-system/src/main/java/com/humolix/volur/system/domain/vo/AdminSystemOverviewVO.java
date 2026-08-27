package com.humolix.volur.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemOverviewVO {

    /** 系统内核与平台版本，如 "Volur Lite v1.0.0-SNAPSHOT" */
    private String kernelVersion;

    /** Spring Boot 框架版本，如 "3.4.3" */
    private String springBootVersion;

    /** JDK / JVM 版本，如 "JDK 25 (Oracle Corporation)" */
    private String javaVersion;

    /** 运行底层环境汇总，如 "Spring Boot 3.4.3 / JDK 25" */
    private String runtimeEnv;

    /** 操作系统环境，如 "Windows 11 (amd64)" */
    private String osInfo;

    /** Redis 缓存连接状态，"CONNECTED" 或 "DISCONNECTED" */
    private String redisStatus;

    /** Redis 响应延迟 (毫秒) */
    private Double redisLatencyMs;

    /** 实时 SSE 推送总线状态，"ONLINE" */
    private String sseStatus;

    /** 系统启动运行总时长 (秒) */
    private Long uptimeSeconds;

    /** JVM 堆内存已用 (MB) */
    private Long heapMemoryUsedMb;

    /** JVM 堆内存最大 (MB) */
    private Long heapMemoryMaxMb;

}
