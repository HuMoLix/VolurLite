package com.humolix.volur.container.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群镜像概览与统计视图 VO")
public class AdminClusterImageStatisticsVO {

    @Builder.Default
    @Schema(description = "镜像总数")
    private Long totalImages = 0L;

    @Builder.Default
    @Schema(description = "总存储占用(字节)")
    private Long totalSizeBytes = 0L;

    @Builder.Default
    @Schema(description = "私有镜像仓库是否在线可用")
    private Boolean registryOnline = false;

    @Builder.Default
    @Schema(description = "私有镜像仓库中托管的镜像总数")
    private Long registryImagesCount = 0L;

}
