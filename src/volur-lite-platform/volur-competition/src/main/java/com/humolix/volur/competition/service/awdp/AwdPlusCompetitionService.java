package com.humolix.volur.competition.service.awdp;

import com.humolix.volur.competition.domain.dto.awdp.AwdPlusPatchUploadRequestDTO;
import com.humolix.volur.competition.domain.vo.awdp.AwdPlusTargetStatusVO;

import java.util.List;

public interface AwdPlusCompetitionService {

    List<AwdPlusTargetStatusVO> listTargets(Long competitionId, Long userId);

    void uploadPatch(Long competitionId, Long userId, AwdPlusPatchUploadRequestDTO request);

}
