package com.humolix.volur.competition.service.stage.awdp;

import com.humolix.volur.competition.domain.dto.stage.awdp.AwdPlusPatchUploadRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.awdp.AwdPlusTargetStatusVO;

import java.util.List;

public interface AwdPlusStageService {

    List<AwdPlusTargetStatusVO> listTargets(Long competitionId, Long userId);

    void uploadPatch(Long competitionId, Long userId, AwdPlusPatchUploadRequestDTO request);

}
