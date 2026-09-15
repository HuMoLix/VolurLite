package com.humolix.volur.challenge.controller.v1.admin.awdp;

import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.challenge.service.admin.awdp.AdminAwdPlusChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端 AWD+ 题库管理", description = "管理员管理攻防兼备增强版 (AWD+) 模式题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/awdp")
@RequiredArgsConstructor
public class AdminAwdPlusChallengeController {

    private final AdminAwdPlusChallengeService awdPlusService;

    @Operation(summary = "分页获取 AWD+ 题目列表")
    @GetMapping
    public ApiResponse<PageResponse<AwdPlusChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取 AWD+ 题目列表成功", awdPlusService.listChallenges(page, size, category, keyword));
    }

    @Operation(summary = "获取指定 AWD+ 题目详情")
    @GetMapping("/{id}")
    public ApiResponse<AwdPlusChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取 AWD+ 题目详情成功", awdPlusService.getChallenge(id));
    }

    @Operation(summary = "创建 AWD+ 题目")
    @Syslog(module = "CHALLENGE", value = "创建 AWD+ 题目", recordParams = true)
    @PostMapping
    public ApiResponse<Long> createChallenge(
            @Valid @RequestBody AwdPlusChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建 AWD+ 题目成功", awdPlusService.createChallenge(request, authorId));
    }

    @Operation(summary = "更新 AWD+ 题目")
    @Syslog(module = "CHALLENGE", value = "更新 AWD+ 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody AwdPlusChallengeUpdateRequestDTO request) {
        awdPlusService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新 AWD+ 题目成功");
    }

    @Operation(summary = "删除 AWD+ 题目")
    @Syslog(module = "CHALLENGE", value = "删除 AWD+ 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        awdPlusService.deleteChallenge(id);
        return ApiResponseEntity.success("删除 AWD+ 题目成功");
    }

}
