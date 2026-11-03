package com.humolix.volur.competition.domain.vo.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositeTeamScoreVO implements Serializable {

    private Integer rank;
    private Long teamId;
    private String teamName;
    private String avatarUrl;
    private Double totalFinalScore;
    private List<CompetitionScoreDetailVO> stageScores;

}
