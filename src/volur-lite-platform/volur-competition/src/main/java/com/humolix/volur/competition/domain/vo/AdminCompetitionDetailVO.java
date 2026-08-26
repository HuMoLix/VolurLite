package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompetitionDetailVO implements Serializable {

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
    private Integer maxTeamMembers;
    private Integer minTeamMembers;
    private Integer maxTeams;
    private Boolean requireTeamAudit;
    private Integer freezeScoreboardMinutes;
    private Boolean isScoreboardFrozen;
    private Boolean allowWriteupSubmission;
    private List<CompetitionStageVO> stages;
    private List<CompetitionFieldSchemaVO> fieldSchemas;
    private Long createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

}
