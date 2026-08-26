package com.humolix.volur.challenge.service.admin.stage.isw;

import com.humolix.volur.challenge.domain.dto.stage.isw.IswChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.isw.IswChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminIswChallengeService {

    PageResponse<IswChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword);

    IswChallengeDetailVO getChallenge(Long id);

    Long createChallenge(IswChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, IswChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
