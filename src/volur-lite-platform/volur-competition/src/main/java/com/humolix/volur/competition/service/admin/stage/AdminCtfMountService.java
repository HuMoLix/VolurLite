package com.humolix.volur.competition.service.admin.stage;

import com.humolix.volur.competition.domain.dto.stage.ctf.CtfChallengeBatchMountRequestDTO;
import com.humolix.volur.competition.domain.dto.stage.ctf.CtfChallengeMountRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.ctf.CtfCompetitionChallengeVO;

import java.util.List;

public interface AdminCtfMountService {

    List<CtfCompetitionChallengeVO> listMountedChallenges(Long competitionId);

    void mountChallenge(Long competitionId, CtfChallengeMountRequestDTO request);

    void batchMountChallenges(Long competitionId, CtfChallengeBatchMountRequestDTO request);

    void unmountChallenge(Long competitionId, Long competitionChallengeId);

}
