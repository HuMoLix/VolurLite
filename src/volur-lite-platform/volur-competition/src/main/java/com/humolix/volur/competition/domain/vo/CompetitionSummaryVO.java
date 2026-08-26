package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
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
public class CompetitionSummaryVO implements Serializable {

    private Long id;
    private String title;
    private String slug;
    private String description;
    private String bannerUrl;
    private CompetitionStatus status;
    private Instant regStartTime;
    private Instant regEndTime;
    private Instant startTime;
    private Instant endTime;
    private Long teamCount;
    private Instant createdAt;

}
