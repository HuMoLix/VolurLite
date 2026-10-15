package com.humolix.volur.competition.domain.vo.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CtfCompetitionChallengeDetailVO implements Serializable {

    private Long id;
    private Long challengeId;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private ChallengeType type;
    private Integer difficulty;
    private Integer initialPoints;
    private Integer currentPoints;
    private Integer solvesCount;
    private String attachmentUrl;
    private String attachmentName;
    private Boolean isContainer;
    private Boolean isSolvedByMyTeam;
    private ChallengeInstanceResponseVO instance;
    private List<String> topTeams;
    private List<CtfChallengeSolveVO> solves;

}
