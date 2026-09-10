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
@Schema(description = "管理端容器集群运行统计视图对象")
public class AdminContainerStatisticsVO {

    @Builder.Default
    @Schema(description = "活跃总实例数")
    private long totalInstances = 0L;

    @Builder.Default
    @Schema(description = "运行中实例数")
    private long runningCount = 0L;

    @Builder.Default
    @Schema(description = "启动中/排队中实例数")
    private long startingCount = 0L;

    @Builder.Default
    @Schema(description = "异常/失败实例数")
    private long failedCount = 0L;

    @Builder.Default
    @Schema(description = "今日累计创建数")
    private long todayCreatedCount = 0L;

    @Schema(description = "当前默认容器编排类型 (DOCKER_SWARM / K3S)")
    private String activeContainerType;

}
