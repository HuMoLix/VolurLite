package com.humolix.volur.challenge.service.admin.stage.ctf;

import com.humolix.volur.challenge.domain.dto.stage.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminCtfChallengeService {

    PageResponse<CtfChallengeListVO> listChallenges(Integer page, Integer size, Long categoryId, String keyword);

    CtfChallengeDetailVO getChallenge(Long id);

    Long createChallenge(CtfChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, CtfChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
