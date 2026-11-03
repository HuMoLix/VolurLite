package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
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
public class TeamInvitationVO implements Serializable {

    private Long id;

    private Long competitionId;

    private String competitionTitle;

    private Long teamId;

    private String teamName;

    private Long captainId;

    private String captainName;

    private MemberRole role;

    private MemberStatus status;

    private Instant invitedAt;

    private String realName;

    private String email;

    private String phone;

}
