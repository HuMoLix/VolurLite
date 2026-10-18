package com.humolix.volur.competition.domain.vo.export;

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
public class CtfSolveDetailExportVO implements Serializable {

    private Long solveId;
    private Long teamId;
    private String teamName;
    private String challengeName;
    private String category;
    private String username;
    private String realName;
    private Integer pointsAwarded;
    private Instant solvedAt;
    private Boolean isFirstBlood;

}
