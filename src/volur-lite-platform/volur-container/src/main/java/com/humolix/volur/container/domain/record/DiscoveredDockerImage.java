package com.humolix.volur.container.domain.record;

/**
 * Docker 节点或引擎扫描发现的镜像摘要元数据
 */
public record DiscoveredDockerImage(
        String repository,
        String tag,
        String imageId,
        long sizeBytes
) {}
