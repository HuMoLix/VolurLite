package com.humolix.volur.challenge.service.admin.stage.ctf;

import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeListResponseVO;
import org.springframework.data.domain.Page;

public interface AdminCtfChallengeService {

    Page<AdminChallengeListResponseVO> pageChallenges(Integer page, Integer size, Long categoryId, String keyword);

    AdminChallengeDetailResponseVO getChallengeDetail(Long id);

    AdminChallengeDetailResponseVO createChallenge(CtfChallengeCreateRequestDTO request, Long authorId);

    AdminChallengeDetailResponseVO updateChallenge(Long id, CtfChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

}
