package com.humolix.volur.competition.domain.vo.scoreboard;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CompetitionScoreDetailVO implements Serializable {

    @JsonProperty("stage")
    private CompetitionType type;

    @JsonProperty("stageName")
    private String typeName;

    private Double rawScore;
    private Double weightPercent;
    private Double weightedScore;

}
