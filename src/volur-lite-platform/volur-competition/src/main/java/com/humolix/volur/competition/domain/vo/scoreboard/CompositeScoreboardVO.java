package com.humolix.volur.competition.domain.vo.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositeScoreboardVO implements Serializable {

    private Long competitionId;
    private String competitionTitle;
    private Boolean isFrozen;
    private List<CompositeTeamScoreVO> rankings;

}
