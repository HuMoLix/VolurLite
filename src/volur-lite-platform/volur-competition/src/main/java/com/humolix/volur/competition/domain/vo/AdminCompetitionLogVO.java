package com.humolix.volur.competition.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 管理端比赛操作日志展示视图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompetitionLogVO implements Serializable {

    private Long id;
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
    private Instant createdAt;

}
