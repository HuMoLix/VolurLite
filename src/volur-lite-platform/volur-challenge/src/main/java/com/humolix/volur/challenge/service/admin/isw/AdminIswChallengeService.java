package com.humolix.volur.challenge.service.admin.isw;

import com.humolix.volur.challenge.domain.dto.isw.IswChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.isw.IswChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminIswChallengeService {

    PageResponse<IswChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword);

    IswChallengeDetailVO getChallenge(Long id);

    Long createChallenge(IswChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, IswChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
