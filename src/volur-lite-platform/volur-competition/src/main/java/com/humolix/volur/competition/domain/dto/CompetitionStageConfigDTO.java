package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.StageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionStageConfigDTO {

    @NotNull(message = "阶段类型不能为空")
    private CompetitionStage stage;

    private String stageName;

    private Integer stageOrder;

    @NotNull(message = "阶段开始时间不能为空")
    private Instant startTime;

    @NotNull(message = "阶段结束时间不能为空")
    private Instant endTime;

    private Double weightPercent;

    private StageStatus status;

    private String stageParamsJson;

}
