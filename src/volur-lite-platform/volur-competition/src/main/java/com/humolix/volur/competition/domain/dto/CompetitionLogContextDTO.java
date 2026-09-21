package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AOP 切面采集快照上下文传输对象（供虚拟线程异步消费）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionLogContextDTO implements Serializable {

    private Long competitionId;
    private Long userId;
    private String username;
    private Long teamId;
    private String teamName;
    private Long challengeId;
    private String challengeTitle;
    private String action;
    private String logType;
    private Boolean status;
    private String operatorIp;
    private String requestMethod;
    private String requestUri;
    private Long durationMs;
    private String params;
    private String result;
    private String errorMessage;
    private String userAgent;

}
