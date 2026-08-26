package com.humolix.volur.challenge.service.admin.stage.awdp;

import com.humolix.volur.challenge.domain.dto.stage.awdp.AwdPlusChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.awdp.AwdPlusChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.stage.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminAwdPlusChallengeService {

    PageResponse<AwdPlusChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword);

    AwdPlusChallengeDetailVO getChallenge(Long id);

    Long createChallenge(AwdPlusChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, AwdPlusChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
