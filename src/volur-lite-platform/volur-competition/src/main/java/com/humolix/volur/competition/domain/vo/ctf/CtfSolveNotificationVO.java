package com.humolix.volur.competition.domain.vo.ctf;

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
public class CtfSolveNotificationVO implements Serializable {

    private Long competitionId;
    private Long challengeId;
    private String challengeTitle;
    private String challengeCategory;
    private Long teamId;
    private String teamName;
    private String teamAvatar;
    private Long userId;
    private String username;
    private String userNickname;
    private Integer solveRank;
    private String bloodType;
    private Integer pointsAwarded;
    private Instant solvedAt;

}
