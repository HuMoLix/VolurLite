package com.humolix.volur.logging.domain.vo;

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
public class AdminLogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String traceId;

    private String serviceName;

    private String level;

    private String logType;

    private String module;

    private String action;

    private String operator;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    private String operatorIp;

    private String requestUri;

    private String requestMethod;

    private String params;

    private String result;

    @Builder.Default
    private Boolean status = true;

    private String errorMessage;

    private Long durationMs;

    private String userAgent;

    private Instant createdAt;

}
