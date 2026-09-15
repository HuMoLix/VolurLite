package com.humolix.volur.competition.service.admin.ctf;

import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeBatchMountRequestDTO;
import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeMountRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;

import java.util.List;

public interface AdminCtfMountService {

    List<CtfCompetitionChallengeVO> listMountedChallenges(Long competitionId);

    void mountChallenge(Long competitionId, CtfChallengeMountRequestDTO request);

    void batchMountChallenges(Long competitionId, CtfChallengeBatchMountRequestDTO request);

    void updateMountedChallenge(Long competitionId, Long competitionChallengeId, CtfChallengeMountRequestDTO request);

    void unmountChallenge(Long competitionId, Long competitionChallengeId);

}
