package com.humolix.volur.training.controller.v1;

import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.training.domain.dto.TrainingFlagSubmitDTO;
import com.humolix.volur.training.domain.vo.TrainingCategoryVO;
import com.humolix.volur.training.domain.vo.TrainingChallengeDetailVO;
import com.humolix.volur.training.domain.vo.TrainingChallengeListVO;
import com.humolix.volur.training.domain.vo.TrainingFlagResultVO;
import com.humolix.volur.training.domain.vo.TrainingLeaderboardVO;
import com.humolix.volur.training.domain.vo.TrainingProgressVO;
import com.humolix.volur.training.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练模块用户端接口。
 * 靶机启动/销毁/延期复用容器模块接口 /api/v1/challenges/instances/{sourceChallengeId}/*，
 * 本控制器只提供运行状态查询（docs/training-api.md §3、§4）。
 */
@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/challenges")
    public ApiResponseEntity<PageResponse<TrainingChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取训练题列表成功",
                trainingService.listChallenges(page, size, categoryId, difficulty, keyword, userId));
    }

    @GetMapping("/categories")
    public ApiResponseEntity<List<TrainingCategoryVO>> getCategories(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取训练分类成功", trainingService.getCategories(userId));
    }

    @GetMapping("/challenges/{id}")
    public ApiResponseEntity<TrainingChallengeDetailVO> getChallenge(
            @PathVariable Long id,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取训练题详情成功", trainingService.getChallenge(id, userId));
    }

    @Syslog(module = "TRAINING", value = "提交训练题 flag", recordParams = true)
    @PostMapping("/challenges/{id}/flag")
    public ApiResponseEntity<TrainingFlagResultVO> submitFlag(
            @PathVariable Long id,
            @Valid @RequestBody TrainingFlagSubmitDTO request,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("提交完成", trainingService.submitFlag(id, userId, request.getFlag()));
    }

    @GetMapping("/challenges/{id}/instance")
    public ApiResponseEntity<ChallengeInstanceResponseVO> getInstance(
            @PathVariable Long id,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取靶机状态成功", trainingService.getInstance(id, userId));
    }

    @GetMapping("/progress")
    public ApiResponseEntity<TrainingProgressVO> getProgress(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取训练进度成功", trainingService.getProgress(userId));
    }

    @GetMapping("/leaderboard")
    public ApiResponseEntity<TrainingLeaderboardVO> getLeaderboard(
            @RequestParam(defaultValue = "50") int limit,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取训练排行榜成功", trainingService.getLeaderboard(limit, userId));
    }

}
