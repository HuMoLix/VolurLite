package com.humolix.volur.challenge.object.vo.stage.ctf;

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
public class CtfTrainingChallengeSolverResponseVO implements Serializable {

    private Long userId;
    private String username;
    private String avatarUrl;
    private Instant solvedAt;

}
