package com.humolix.volur.challenge.controller.v1.admin.stage.awd;

import com.humolix.volur.challenge.domain.dto.stage.awd.AwdChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.awd.AwdChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.challenge.domain.vo.stage.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.awd.AwdChallengeListVO;
import com.humolix.volur.challenge.service.admin.stage.awd.AdminAwdChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端 AWD 题库管理", description = "管理员管理攻防对抗 (AWD) 模式题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/awd")
@RequiredArgsConstructor
public class AdminAwdChallengeController {

    private final AdminAwdChallengeService awdService;

    @Operation(summary = "分页获取 AWD 题目列表")
    @GetMapping
    public ApiResponse<PageResponse<AwdChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AwdCategory category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取 AWD 题目列表成功", awdService.listChallenges(page, size, category, keyword));
    }

    @Operation(summary = "获取指定 AWD 题目详情")
    @GetMapping("/{id}")
    public ApiResponse<AwdChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取 AWD 题目详情成功", awdService.getChallenge(id));
    }

    @Operation(summary = "创建 AWD 题目")
    @PostMapping
    public ApiResponse<Long> createChallenge(
            @Valid @RequestBody AwdChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建 AWD 题目成功", awdService.createChallenge(request, authorId));
    }

    @Operation(summary = "更新 AWD 题目")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody AwdChallengeUpdateRequestDTO request) {
        awdService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新 AWD 题目成功");
    }

    @Operation(summary = "删除 AWD 题目")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        awdService.deleteChallenge(id);
        return ApiResponseEntity.success("删除 AWD 题目成功");
    }

}
