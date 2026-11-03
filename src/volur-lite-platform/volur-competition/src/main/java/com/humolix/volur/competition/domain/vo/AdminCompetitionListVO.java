package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
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
public class AdminCompetitionListVO implements Serializable {

    private Long id;
    private String title;
    private String slug;
    private CompetitionType type;
    private CompetitionStatus status;
    private Instant startTime;
    private Instant endTime;
    private Long teamCount;
    private Long participantCount;
    private Instant createdAt;

}
