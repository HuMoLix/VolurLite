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
    @Schema(description = "赛题镜像数")
    private Long challengeImages = 0L;

    @Builder.Default
    @Schema(description = "基础与系统镜像数")
    private Long baseImages = 0L;

    @Builder.Default
    @Schema(description = "总存储占用(字节)")
    private Long totalSizeBytes = 0L;

    @Builder.Default
    @Schema(description = "格式化总存储占用")
    private String totalSizeFormatted = "0 B";

    @Builder.Default
    @Schema(description = "已覆盖节点数")
    private Integer totalNodesCovered = 0;

    @Builder.Default
    @Schema(description = "同步或拉取中的镜像数")
    private Long syncingImages = 0L;

}
