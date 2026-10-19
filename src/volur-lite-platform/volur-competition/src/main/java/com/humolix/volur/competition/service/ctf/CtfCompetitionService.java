package com.humolix.volur.competition.service.ctf;

import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;

import com.humolix.volur.competition.domain.vo.ctf.CtfTeamSolveRecordVO;

import java.util.List;

public interface CtfCompetitionService {

    List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId);

    CtfCompetitionChallengeDetailVO getChallengeDetail(Long competitionId, Long competitionChallengeId, Long userId);

    boolean submitFlag(Long competitionId, Long competitionChallengeId, Long userId, CtfFlagSubmitRequestDTO request, String ip);

    ChallengeInstanceResponseVO startInstance(Long competitionId, Long competitionChallengeId, Long userId);

    void stopInstance(Long competitionId, Long competitionChallengeId, Long userId);

    ChallengeInstanceResponseVO extendInstance(Long competitionId, Long competitionChallengeId, Long userId);

    ChallengeInstanceResponseVO getInstance(Long competitionId, Long competitionChallengeId, Long userId);

    List<CtfTeamSolveRecordVO> getTeamSolves(Long competitionId, Long teamId);

}
