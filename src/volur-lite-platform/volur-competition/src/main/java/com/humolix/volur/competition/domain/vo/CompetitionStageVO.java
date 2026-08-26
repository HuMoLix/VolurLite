package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.StageStatus;
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
public class CompetitionStageVO implements Serializable {

    private Long id;
    private CompetitionStage stage;
    private String stageName;
    private Integer stageOrder;
    private Instant startTime;
    private Instant endTime;
    private Double weightPercent;
    private StageStatus status;
    private String stageParamsJson;

}
