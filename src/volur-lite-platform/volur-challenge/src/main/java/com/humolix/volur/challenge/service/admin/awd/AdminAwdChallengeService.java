package com.humolix.volur.challenge.service.admin.awd;

import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminAwdChallengeService {

    PageResponse<AwdChallengeListVO> listChallenges(Integer page, Integer size, AwdCategory category, String keyword);

    AwdChallengeDetailVO getChallenge(Long id);

    Long createChallenge(AwdChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, AwdChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
