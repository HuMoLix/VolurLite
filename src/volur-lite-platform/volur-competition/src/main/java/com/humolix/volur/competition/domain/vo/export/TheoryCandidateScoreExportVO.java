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
public class TheoryCandidateScoreExportVO implements Serializable {

    private Integer rank;
    private Long teamId;
    private String teamName;
    private Long userId;
    private String username;
    private String realName;
    private com.humolix.volur.user.domain.enums.Gender gender;
    private String phone;
    private String email;
    private String idCard;
    private String employeeNumber;
    private String department;
    private String position;
    private Boolean isSubmitted;
    private Integer correctCount;
    private Integer totalQuestions;
    private Integer totalScore;
    private Integer scoreObtained;
    private Double accuracyRate;
    private Integer violationCount;
    private Instant submittedAt;

}
