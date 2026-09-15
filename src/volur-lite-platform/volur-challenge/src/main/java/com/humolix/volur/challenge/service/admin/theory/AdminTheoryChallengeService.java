package com.humolix.volur.challenge.service.admin.theory;

import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminTheoryChallengeService {

    PageResponse<TheoryChallengeListVO> listChallenges(Integer page, Integer size, TheoryType type, String category, String keyword);

    TheoryChallengeDetailVO getChallenge(Long id);

    Long createChallenge(TheoryChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, TheoryChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
