package com.humolix.volur.logging.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端系统日志查询请求 DTO")
public class AdminLogQueryDTO {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "模糊搜索关键词 (操作者/IP/动作/URI)")
    private String keyword;

    @Schema(description = "日志级别 (INFO/WARN/ERROR)")
    private String level;

    @Schema(description = "业务模块 (AUTH/USER/CHALLENGE/COMPETITION/CONTAINER/SYSTEM/NOTIFICATION/VERIFICATION)")
    private String module;

    @Schema(description = "执行状态 (true: 成功, false: 失败)")
    private Boolean status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "起始时间")
    private Instant startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "截止时间")
    private Instant endTime;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "排序方向", example = "desc")
    private String sortOrder = "desc";

}
