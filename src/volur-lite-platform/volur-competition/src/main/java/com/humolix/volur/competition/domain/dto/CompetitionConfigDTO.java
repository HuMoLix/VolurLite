package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionConfigDTO {

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

    private java.time.Instant writeupStartTime;

    private java.time.Instant writeupEndTime;

    private String regRequirements;
 
    private Boolean enableBloodBonus;

    private Integer firstBloodBonusPercentage;

    private Integer secondBloodBonusPercentage;

    private Integer thirdBloodBonusPercentage;
}
