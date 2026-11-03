package com.humolix.volur.container.domain.record;

/**
 * Docker 引擎活跃容器简要运行状态信息
 */
public record DockerContainerBriefInfo(
        String id,
        String name,
        String status,
        String state,
        String image
) {}
