package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionUpdateRequestDTO {

    private String title;

    private String slug;

    private String description;

    private String bannerUrl;

    private CompetitionStatus status;

    private Instant regStartTime;

    private Instant regEndTime;

    private Instant startTime;

    private Instant endTime;

    private CompetitionConfigDTO config;

    private List<CompetitionStageConfigDTO> stageConfigs;

    private List<CompetitionFieldSchemaDTO> fieldSchemas;

}
