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
public class AdminCompetitionDetailVO implements Serializable {

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
    private Integer maxTeams;
    private Boolean requireTeamAudit;
    private Integer freezeScoreboardMinutes;
    private Boolean isScoreboardFrozen;
    private Boolean allowWriteupSubmission;
    private Integer maxContainersPerTeam;
    private String writeupTemplateUrl;
    private String writeupTemplateName;
    private Instant writeupStartTime;
    private Instant writeupEndTime;
    private String regRequirements;
    private Boolean enableBloodBonus;
    private Integer firstBloodBonusPercentage;
    private Integer secondBloodBonusPercentage;
    private Integer thirdBloodBonusPercentage;
    private Long createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

}
