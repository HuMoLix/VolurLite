package com.humolix.volur.competition.service.awd;

import com.humolix.volur.competition.domain.dto.awd.AwdFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.awd.AwdCurrentRoundVO;

public interface AwdCompetitionService {

    AwdCurrentRoundVO getCurrentRound(Long competitionId, Long userId);

    boolean submitFlag(Long competitionId, Long userId, AwdFlagSubmitRequestDTO request);

}
