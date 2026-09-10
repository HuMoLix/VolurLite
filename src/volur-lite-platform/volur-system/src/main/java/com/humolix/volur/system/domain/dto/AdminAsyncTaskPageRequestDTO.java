package com.humolix.volur.system.domain.dto;

import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端异步任务分页列表查询请求参数")
public class AdminAsyncTaskPageRequestDTO extends PageRequestDTO {

    @Schema(description = "模糊搜索关键词（任务名称、错误信息、操作人）")
    private String keyword;

    @Schema(description = "任务类型筛选")
    private AsyncTaskType taskType;

    @Schema(description = "任务状态筛选")
    private AsyncTaskStatus status;

    @Schema(description = "起始创建时间")
    private Instant startTime;

    @Schema(description = "截止创建时间")
    private Instant endTime;

}
