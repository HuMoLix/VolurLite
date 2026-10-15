package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.TeamStatus;
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
public class AdminTeamListVO implements Serializable {

    private Long id;
    private Long competitionId;
    private String teamName;
    private Long captainId;
    private String captainUsername;
    private Integer memberCount;
    private TeamStatus status;
    private Integer score;
    private String auditReason;
    private Instant createdAt;

}
