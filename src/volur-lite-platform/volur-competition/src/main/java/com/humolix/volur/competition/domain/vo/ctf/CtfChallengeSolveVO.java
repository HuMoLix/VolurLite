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
public class CtfChallengeSolveVO implements Serializable {

    private Long teamId;
    private String teamName;
    private String avatarUrl;
    private Instant solvedAt;
    private Integer points;
    private Boolean isFirstBlood;

}
