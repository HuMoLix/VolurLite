package com.humolix.volur.competition.service.stage.awd;

import com.humolix.volur.competition.domain.dto.stage.awd.AwdFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.awd.AwdCurrentRoundVO;

public interface AwdStageService {

    AwdCurrentRoundVO getCurrentRound(Long competitionId, Long userId);

    boolean submitFlag(Long competitionId, Long userId, AwdFlagSubmitRequestDTO request);

}
