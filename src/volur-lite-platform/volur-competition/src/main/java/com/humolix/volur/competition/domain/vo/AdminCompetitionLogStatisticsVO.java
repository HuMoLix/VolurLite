package com.humolix.volur.competition.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理端单场比赛日志统计指标 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompetitionLogStatisticsVO implements Serializable {

    @Builder.Default
    private Long totalLogs = 0L;

    @Builder.Default
    private Long containerLogs = 0L;

    @Builder.Default
    private Long flagLogs = 0L;

    @Builder.Default
    private Long errorLogs = 0L;

}
