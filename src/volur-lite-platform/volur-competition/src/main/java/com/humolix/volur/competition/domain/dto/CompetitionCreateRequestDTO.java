package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionCreateRequestDTO {

    @NotBlank(message = "竞赛标题不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String title;

    @NotBlank(message = "竞赛短链接不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String slug;

    @NotNull(message = "竞赛类型不能为空")
    private CompetitionType type;

    private String description;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String bannerUrl;

    private CompetitionStatus status;

    private Instant regStartTime;

    private Instant regEndTime;

    @NotNull(message = "竞赛开始时间不能为空")
    private Instant startTime;

    @NotNull(message = "竞赛结束时间不能为空")
    private Instant endTime;

    private CompetitionConfigDTO config;

}
