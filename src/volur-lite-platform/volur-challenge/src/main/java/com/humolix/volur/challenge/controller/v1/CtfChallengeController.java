package com.humolix.volur.challenge.controller.v1;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.challenge.object.dto.FlagSubmissionRequestDTO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengePageResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeSolverResponseVO;
import com.humolix.volur.challenge.service.stage.CtfChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenges/ctf")
public class CtfChallengeController {

    private final CtfChallengeService challengeService;

    @GetMapping("/categories")
    public ApiResponse<List<CtfChallengeCategoryResponseVO>> listCategories() {
        return ApiResponseEntity.ok("获取题目分类成功", challengeService.listCategories());
    }

    @GetMapping("/training")
    public ApiResponse<Page<CtfTrainingChallengePageResponseVO>> pageTrainingChallenges(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<CtfTrainingChallengePageResponseVO> result = challengeService.pageTrainingChallenges(page, size, categoryId, keyword, userId);
        return ApiResponseEntity.ok("获取训练场题目列表成功", result);
    }

    @GetMapping("/training/{id}")
    public ApiResponse<CtfTrainingChallengeDetailResponseVO> getTrainingChallengeDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        CtfTrainingChallengeDetailResponseVO detail = challengeService.getTrainingChallengeDetail(id, userId);
        return ApiResponseEntity.ok("获取题目详情成功", detail);
    }

    @GetMapping("/training/{id}/solvers")
    public ApiResponse<Page<CtfTrainingChallengeSolverResponseVO>> pageSolvers(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<CtfTrainingChallengeSolverResponseVO> solvers = challengeService.pageSolvers(id, page, size);
        return ApiResponseEntity.ok("获取解题榜成功", solvers);
    }

    @PostMapping("/training/{id}/submit")
    public ApiResponse<Boolean> submitFlag(
            @PathVariable Long id,
            @Valid @RequestBody FlagSubmissionRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isCorrect = challengeService.submitFlag(id, request, userId);
        if (isCorrect) {
            return ApiResponseEntity.ok("恭喜你！Flag 正确！", true);
        } else {
            return ApiResponseEntity.badRequest("Flag 错误，请再接再厉！", false);
        }
    }

}
