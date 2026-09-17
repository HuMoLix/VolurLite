package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.TeamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeamDetailVO implements Serializable {

    private Long id;
    private Long competitionId;
    private String competitionTitle;
    private String teamName;
    private String avatarUrl;
    private Long captainId;
    private String captainUsername;
    private TeamStatus status;
    private String auditReason;
    private List<CompetitionMemberVO> members;
    private Instant createdAt;

}
