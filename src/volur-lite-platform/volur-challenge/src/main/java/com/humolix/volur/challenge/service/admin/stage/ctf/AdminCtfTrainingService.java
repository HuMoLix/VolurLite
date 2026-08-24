package com.humolix.volur.challenge.service.admin.stage.ctf;

import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminTrainingChallengeListResponseVO;
import org.springframework.data.domain.Page;

public interface AdminCtfTrainingService {

    Page<AdminTrainingChallengeListResponseVO> pageTrainingChallenges(Integer page, Integer size, String keyword);

    AdminTrainingChallengeListResponseVO createTrainingChallenge(CtfTrainingChallengeCreateRequestDTO request);

    AdminTrainingChallengeListResponseVO updateTrainingChallenge(Long id, CtfTrainingChallengeUpdateRequestDTO request);

    void toggleActive(Long id);

    void deleteTrainingChallenge(Long id);

}
