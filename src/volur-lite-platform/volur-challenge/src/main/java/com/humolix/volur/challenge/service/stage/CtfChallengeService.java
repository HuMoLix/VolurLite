package com.humolix.volur.challenge.service.stage;

import com.humolix.volur.challenge.object.dto.FlagSubmissionRequestDTO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengePageResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeSolverResponseVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CtfChallengeService {

    List<CtfChallengeCategoryResponseVO> listCategories();

    Page<CtfTrainingChallengePageResponseVO> pageTrainingChallenges(Integer page, Integer size, Long categoryId, String keyword, Long userId);

    CtfTrainingChallengeDetailResponseVO getTrainingChallengeDetail(Long trainingChallengeId, Long userId);

    Page<CtfTrainingChallengeSolverResponseVO> pageSolvers(Long trainingChallengeId, Integer page, Integer size);

    boolean submitFlag(Long trainingChallengeId, FlagSubmissionRequestDTO request, Long userId);

}
