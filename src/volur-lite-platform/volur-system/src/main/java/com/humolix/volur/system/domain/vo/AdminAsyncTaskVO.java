package com.humolix.volur.system.domain.vo;

import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端异步任务详情视图对象")
public class AdminAsyncTaskVO implements Serializable {

    @Schema(description = "任务 ID")
    private Long id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型编码")
    private AsyncTaskType taskType;

    @Schema(description = "任务类型描述")
    private String taskTypeDesc;

    @Schema(description = "任务执行状态")
    private AsyncTaskStatus status;

    @Schema(description = "任务状态描述")
    private String statusDesc;

    @Schema(description = "当前执行进度百分比 (0-100)")
    @Builder.Default
    private Integer progress = 0;

    @Schema(description = "当前步骤与阶段提示信息")
    private String statusMessage;

    @Schema(description = "异常报错与堆栈信息")
    private String errorMessage;

    @Schema(description = "任务提交入参 (JSON 字符串)")
    private String parameters;

    @Schema(description = "任务执行结果数据 (JSON 字符串)")
    private String resultData;

    @Schema(description = "操作人用户 ID")
    private Long operatorId;

    @Schema(description = "操作人用户名")
    private String operatorName;

    @Schema(description = "任务创建排队时间")
    private Instant createdAt;

    @Schema(description = "任务正式启动时间")
    private Instant startedAt;

    @Schema(description = "任务完成或失败时间")
    private Instant finishedAt;

    @Schema(description = "总耗时 (毫秒)")
    private Long executionDurationMs;

}
