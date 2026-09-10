package com.humolix.volur.system.domain.vo;

import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
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
@Schema(description = "管理端异步任务提交凭据视图对象")
public class AdminAsyncTaskSubmitVO implements Serializable {

    @Schema(description = "异步任务 ID")
    private Long taskId;

    @Schema(description = "异步任务名称")
    private String taskName;

    @Schema(description = "异步任务类型")
    private AsyncTaskType taskType;

    @Schema(description = "当前状态")
    private AsyncTaskStatus status;

}
