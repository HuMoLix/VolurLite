package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.vo.scoreboard.CompositeScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfScoreboardVO;

public interface CompetitionScoreboardService {

    CompositeScoreboardVO getCompositeScoreboard(Long competitionId);

    CtfScoreboardVO getCtfScoreboard(Long competitionId);

}
