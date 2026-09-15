package com.humolix.volur.competition.service.isw;

import com.humolix.volur.competition.domain.dto.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.isw.IswCompetitionTopologyVO;

import java.util.List;

public interface IswCompetitionService {

    List<IswCompetitionTopologyVO> listScenarios(Long competitionId, Long userId);

    boolean submitFlag(Long competitionId, Long userId, IswFlagSubmitRequestDTO request);

}
