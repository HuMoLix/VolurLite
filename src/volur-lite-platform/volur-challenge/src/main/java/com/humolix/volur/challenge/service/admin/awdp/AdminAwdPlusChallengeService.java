package com.humolix.volur.challenge.service.admin.awdp;

import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminAwdPlusChallengeService {

    PageResponse<AwdPlusChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword);

    AwdPlusChallengeDetailVO getChallenge(Long id);

    Long createChallenge(AwdPlusChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, AwdPlusChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
