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
public class CtfTeamRankExportVO implements Serializable {

    private Integer rank;
    private Long teamId;
    private String teamName;
    private String organization;
    private String leaderUsername;
    private String leaderRealName;
    private com.humolix.volur.user.domain.enums.Gender leaderGender;
    private String leaderPhone;
    private String leaderEmail;
    private String leaderIdCard;
    private String leaderEmployeeNumber;
    private String leaderDepartment;
    private String leaderPosition;
    private Integer score;
    private Integer solvesCount;
    private Instant lastSolveTime;

}
