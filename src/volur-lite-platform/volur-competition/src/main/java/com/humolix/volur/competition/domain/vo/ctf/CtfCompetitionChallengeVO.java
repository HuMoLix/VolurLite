package com.humolix.volur.competition.domain.vo.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeType;
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
public class CtfCompetitionChallengeVO implements Serializable {

    private Long id; // competition challenge id
    private Long challengeId; // bank challenge id
    private String name;
    private Long categoryId;
    private String categoryName;
    private ChallengeType type;
    private Integer difficulty;
    private Integer initialPoints;
    private Integer minPoints;
    private Integer decayCount;
    private Integer currentPoints;
    private Integer solvesCount;
    private Boolean isSolvedByMyTeam;
    private Integer sortOrder;
    private List<String> topTeams;

}
