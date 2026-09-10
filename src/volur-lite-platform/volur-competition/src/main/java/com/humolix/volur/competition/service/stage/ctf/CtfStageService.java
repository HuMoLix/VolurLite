package com.humolix.volur.competition.service.stage.ctf;

import com.humolix.volur.competition.domain.dto.stage.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.stage.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;

import java.util.List;

public interface CtfStageService {

    List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId);

    List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long stageId, Long userId);

    CtfCompetitionChallengeDetailVO getChallengeDetail(Long competitionId, Long competitionChallengeId, Long userId);

    boolean submitFlag(Long competitionId, Long competitionChallengeId, Long userId, CtfFlagSubmitRequestDTO request, String ip);

    ChallengeInstanceResponseVO startInstance(Long competitionId, Long competitionChallengeId, Long userId);

    void stopInstance(Long competitionId, Long competitionChallengeId, Long userId);

    ChallengeInstanceResponseVO extendInstance(Long competitionId, Long competitionChallengeId, Long userId);

}
