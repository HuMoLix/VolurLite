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
public class CtfTeamSolveRecordVO implements Serializable {

    private Long id;
    private Long challengeId;
    private String challengeName;
    private String category;
    private String submissionMode;
    private Long userId;
    private String username;
    private Integer points;
    private Boolean isFirstBlood;
    private Instant solvedAt;

}
