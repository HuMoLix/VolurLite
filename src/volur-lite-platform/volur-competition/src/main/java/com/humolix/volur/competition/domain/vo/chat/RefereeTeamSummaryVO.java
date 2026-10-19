package com.humolix.volur.competition.domain.vo.chat;

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
public class RefereeTeamSummaryVO implements Serializable {

    private Long teamId;

    private String teamName;

    private String teamAvatar;

    private String lastSenderName;

    private String lastSenderRole;

    private String lastContent;

    private Instant lastCreatedAt;

    @Builder.Default
    private Long totalMessages = 0L;

}
