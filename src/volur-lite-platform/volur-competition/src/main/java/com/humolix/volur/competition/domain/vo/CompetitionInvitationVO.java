package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionInvitationVO {

    private Long id;

    private Long competitionId;

    private String competitionTitle;

    private Long teamId;

    private String teamName;

    private Long inviterId;

    private String inviterUsername;

    private Long inviteeId;

    private String inviteeUsername;

    private InvitationStatus status;

    private String remark;

    private Instant expireTime;

    private Instant createdAt;

}
