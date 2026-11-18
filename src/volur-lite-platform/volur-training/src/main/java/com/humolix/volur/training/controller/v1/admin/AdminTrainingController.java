package com.humolix.volur.training.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.training.domain.dto.TrainingChallengeUpdateDTO;
import com.humolix.volur.training.domain.dto.TrainingImportRequestDTO;
import com.humolix.volur.training.domain.vo.TrainingChallengeListVO;
import com.humolix.volur.training.domain.vo.TrainingImportResultVO;
import com.humolix.volur.training.service.TrainingAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 训练池管理端接口（/api/v1/admin/** 需 ADMIN 角色，见 volur-auth SecurityConfig）。
 * 选题浏览复用 GET /api/v1/admin/challenges/ctf。
 */
@RestController
@RequestMapping("/api/v1/admin/training")
@RequiredArgsConstructor
public class AdminTrainingController {

    private final TrainingAdminService trainingAdminService;

    @GetMapping("/challenges")
    public ApiResponseEntity<PageResponse<TrainingChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponseEntity.ok("获取训练池列表成功",
                trainingAdminService.listChallenges(page, size, keyword, status));
    }

    @Syslog(module = "TRAINING", value = "批量导入训练题", recordParams = true)
    @PostMapping("/challenges/import")
    public ApiResponseEntity<TrainingImportResultVO> importChallenges(
            @Valid @RequestBody TrainingImportRequestDTO request,
            @CurrentUserId Long adminId) {
        return ApiResponseEntity.ok("训练题导入处理完成",
                trainingAdminService.importChallenges(request, adminId));
    }

    @Syslog(module = "TRAINING", value = "更新训练题", recordParams = true)
    @PatchMapping("/challenges/{id}")
    public ApiResponseEntity<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody TrainingChallengeUpdateDTO request) {
        trainingAdminService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新训练题成功");
    }

    @Syslog(module = "TRAINING", value = "移除训练题", recordParams = true)
    @DeleteMapping("/challenges/{id}")
    public ApiResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        trainingAdminService.deleteChallenge(id);
        return ApiResponseEntity.success("移除训练题成功");
    }

}
