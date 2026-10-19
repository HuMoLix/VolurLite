package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.Instant;

/**
 * 管理端比赛操作日志查询入参 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompetitionLogQueryDTO implements Serializable {

    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private String teamName;
    private String challengeTitle;
    private String logType;
    private Boolean status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endTime;

    private String sortBy = "createdAt";
    private String sortOrder = "desc";

}
