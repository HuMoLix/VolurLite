package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.vo.scoreboard.CompositeScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfStageScoreboardVO;

public interface CompetitionScoreboardService {

    CompositeScoreboardVO getCompositeScoreboard(Long competitionId);

    CtfStageScoreboardVO getCtfStageScoreboard(Long competitionId);

}
