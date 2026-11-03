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
public class CompetitionDetailVO implements Serializable {

    private Long id;
    private String title;
    private String slug;
    private String description;
    private String bannerUrl;
    private CompetitionType type;
    private CompetitionStatus status;
    private Instant regStartTime;
    private Instant regEndTime;
    private Instant startTime;
    private Instant endTime;
    private Integer maxLeaders;
    private Integer maxTeamMembers;
    private Integer minTeamMembers;
    private Boolean requireTeamAudit;
    private Boolean allowWriteupSubmission;
    private String regRequirements;
    private Long teamCount;
    private Instant createdAt;

}
