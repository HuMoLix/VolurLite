package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionConfigDTO {

    private Integer maxTeamMembers;

    private Integer minTeamMembers;

    private Integer maxTeams;

    private Boolean requireTeamAudit;

    private Integer freezeScoreboardMinutes;

    private Boolean isScoreboardFrozen;

    private Boolean allowWriteupSubmission;

}
