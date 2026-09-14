package com.humolix.volur.competition.event;

import com.humolix.volur.competition.domain.enums.MemberRole;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class TeamInvitationSentEvent extends ApplicationEvent {

    private final Long competitionId;
    private final String competitionSlug;
    private final String competitionTitle;
    private final Long teamId;
    private final String teamName;
    private final Long inviterId;
    private final String inviterName;
    private final Long inviteeId;
    private final MemberRole memberRole;
    private final String remark;
    private final Instant expireTime;

    public TeamInvitationSentEvent(Object source, Long competitionId, String competitionSlug, String competitionTitle,
                                   Long teamId, String teamName, Long inviterId, String inviterName,
                                   Long inviteeId, MemberRole memberRole, String remark, Instant expireTime) {
        super(source);
        this.competitionId = competitionId;
        this.competitionSlug = competitionSlug;
        this.competitionTitle = competitionTitle;
        this.teamId = teamId;
        this.teamName = teamName;
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.inviteeId = inviteeId;
        this.memberRole = memberRole;
        this.remark = remark;
        this.expireTime = expireTime;
    }

}
