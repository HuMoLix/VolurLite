package com.humolix.volur.challenge.controller.v1.admin;

import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminTrainingChallengeListResponseVO;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfTrainingService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/training/ctf")
public class AdminCtfTrainingController {

    private final AdminCtfTrainingService trainingService;

    @GetMapping
    public ApiResponse<Page<AdminTrainingChallengeListResponseVO>> pageTrainingChallenges(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("分页查询训练场题目成功", trainingService.pageTrainingChallenges(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<AdminTrainingChallengeListResponseVO> createTrainingChallenge(
            @Valid @RequestBody CtfTrainingChallengeCreateRequestDTO request) {
        return ApiResponseEntity.created("添加题目至训练场成功", trainingService.createTrainingChallenge(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminTrainingChallengeListResponseVO> updateTrainingChallenge(
            @PathVariable Long id,
            @Valid @RequestBody CtfTrainingChallengeUpdateRequestDTO request) {
        return ApiResponseEntity.ok("更新训练场题目配置成功", trainingService.updateTrainingChallenge(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        trainingService.toggleActive(id);
        return ApiResponseEntity.success("切换状态成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTrainingChallenge(@PathVariable Long id) {
        trainingService.deleteTrainingChallenge(id);
        return ApiResponseEntity.success("移除训练场题目成功");
    }

}
