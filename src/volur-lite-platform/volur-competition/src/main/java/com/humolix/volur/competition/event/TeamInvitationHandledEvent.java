package com.humolix.volur.competition.event;

import com.humolix.volur.competition.domain.enums.MemberStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TeamInvitationHandledEvent extends ApplicationEvent {

    private final Long competitionId;
    private final String competitionTitle;
    private final Long teamId;
    private final String teamName;
    private final Long captainId;
    private final Long memberUserId;
    private final String memberUsername;
    private final MemberStatus memberStatus;

    public TeamInvitationHandledEvent(Object source, Long competitionId, String competitionTitle,
                                      Long teamId, String teamName, Long captainId, Long memberUserId,
                                      String memberUsername, MemberStatus memberStatus) {
        super(source);
        this.competitionId = competitionId;
        this.competitionTitle = competitionTitle;
        this.teamId = teamId;
        this.teamName = teamName;
        this.captainId = captainId;
        this.memberUserId = memberUserId;
        this.memberUsername = memberUsername;
        this.memberStatus = memberStatus;
    }

}
