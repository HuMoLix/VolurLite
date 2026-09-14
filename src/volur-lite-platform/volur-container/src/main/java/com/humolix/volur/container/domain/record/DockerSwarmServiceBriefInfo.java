package com.humolix.volur.container.domain.record;

/**
 * Docker Swarm 活跃服务简要运行状态信息
 */
public record DockerSwarmServiceBriefInfo(
        String id,
        String name,
        String image,
        String replicas
) {}
