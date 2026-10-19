package com.humolix.volur.competition.domain.vo.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CtfTeamRankVO implements Serializable {

    private Integer rank;
    private Long teamId;
    private String teamName;
    private String avatarUrl;
    private Integer score;
    private Integer solvesCount;
    private Instant lastSolveTime;

}
