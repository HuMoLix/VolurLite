package com.humolix.volur.competition.domain.vo.ctf;

import com.humolix.volur.competition.domain.enums.SubmissionStatus;
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
public class CtfCompetitionSubmissionVO implements Serializable {

    private Long id;
    private Long userId;
    private String username;
    private Long teamId;
    private String teamName;
    private Long challengeId;
    private String challengeName;
    private String challengeCategory;
    private String submittedFlag;
    private SubmissionStatus status;
    private Instant createdAt;

}
