package com.humolix.volur.competition.service.stage.isw;

import com.humolix.volur.competition.domain.dto.stage.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.isw.IswCompetitionTopologyVO;

import java.util.List;

public interface IswStageService {

    List<IswCompetitionTopologyVO> listScenarios(Long competitionId, Long userId);

    boolean submitFlag(Long competitionId, Long userId, IswFlagSubmitRequestDTO request);

}
