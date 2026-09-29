package com.humolix.volur.logging.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统日志响应视图 VO")
public class AdminLogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "日志唯一 ID (雪花算法)")
    private Long id;

    @Schema(description = "链路追踪 Trace ID")
    private String traceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "日志级别 (INFO/WARN/ERROR)")
    private String level;

    @Schema(description = "日志类型 (OPERATE/AUTH/SECURITY/SYSTEM)")
    private String logType;

    @Schema(description = "所属业务模块")
    private String module;

    @Schema(description = "操作动作描述")
    private String action;

    @Schema(description = "操作人用户名")
    private String operator;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "操作人用户 ID")
    private Long operatorId;

    @Schema(description = "客户端真实 IP")
    private String operatorIp;

    @Schema(description = "请求 URI")
    private String requestUri;

    @Schema(description = "HTTP 请求方法 (GET/POST/PUT/DELETE 等)")
    private String requestMethod;

    @Schema(description = "请求参数 (JSON 字符串)")
    private String params;

    @Schema(description = "响应结果 (JSON 字符串)")
    private String result;

    @Builder.Default
    @Schema(description = "执行结果状态 (true: 成功, false: 失败)")
    private Boolean status = true;

    @Schema(description = "异常报错与堆栈信息")
    private String errorMessage;

    @Schema(description = "执行耗时 (毫秒)")
    private Long durationMs;

    @Schema(description = "客户端 User-Agent")
    private String userAgent;

    @Schema(description = "日志创建记录时间")
    private Instant createdAt;

}
