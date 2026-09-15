package com.humolix.volur.challenge.service.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminCtfChallengeService {

    PageResponse<CtfChallengeListVO> listChallenges(Integer page, Integer size, Long categoryId, String keyword);

    CtfChallengeDetailVO getChallenge(Long id);

    Long createChallenge(CtfChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, CtfChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
