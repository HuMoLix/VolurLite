package com.humolix.volur.logging.domain.vo;

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
@Schema(description = "系统日志统计指标 VO")
public class AdminLogStatisticsVO implements Serializable {

    @Builder.Default
    @Schema(description = "总日志流水条数")
    private long totalLogs = 0L;

    @Builder.Default
    @Schema(description = "认证与鉴权日志数")
    private long authLogs = 0L;

    @Builder.Default
    @Schema(description = "高危安全拦截日志数")
    private long securityLogs = 0L;

    @Builder.Default
    @Schema(description = "系统异常报错日志数")
    private long errorLogs = 0L;

}
