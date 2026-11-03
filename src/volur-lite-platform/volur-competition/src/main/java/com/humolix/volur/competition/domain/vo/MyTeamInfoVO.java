package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.MemberRole;
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
public class MyTeamInfoVO implements Serializable {

    private Long teamId;
    private Long competitionId;
    private String competitionTitle;
    private String teamName;
    private String avatarUrl;
    private String description;
    private String organization;
    private com.humolix.volur.competition.domain.enums.OrganizationType organizationType;
    private TeamStatus status;
    private String auditReason;
    private MemberRole myRole;
    private Boolean isCaptain;
    private List<CompetitionMemberVO> members;
    private Map<String, String> myRegistrationData;
    private Instant createdAt;

}
