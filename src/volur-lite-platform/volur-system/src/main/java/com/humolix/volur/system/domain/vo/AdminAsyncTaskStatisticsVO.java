package com.humolix.volur.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端异步任务全局统计视图对象")
public class AdminAsyncTaskStatisticsVO implements Serializable {

    @Schema(description = "总任务数")
    @Builder.Default
    private long totalCount = 0;

    @Schema(description = "正在执行中的任务数")
    @Builder.Default
    private long runningCount = 0;

    @Schema(description = "排队等待中的任务数")
    @Builder.Default
    private long pendingCount = 0;

    @Schema(description = "已成功完成的任务数")
    @Builder.Default
    private long successCount = 0;

    @Schema(description = "执行失败的任务数")
    @Builder.Default
    private long failedCount = 0;

}
