package com.humolix.volur.competition.domain.vo.scoreboard;

import com.humolix.volur.competition.domain.enums.CompetitionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageScoreDetailVO implements Serializable {

    private CompetitionType stage;
    private String stageName;
    private Double rawScore;
    private Double weightPercent;
    private Double weightedScore;

}
