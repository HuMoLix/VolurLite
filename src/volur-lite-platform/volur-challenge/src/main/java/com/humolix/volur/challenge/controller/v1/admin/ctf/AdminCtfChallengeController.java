package com.humolix.volur.challenge.controller.v1.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfChallengeService;
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

@Tag(name = "管理端 CTF 题库管理", description = "管理员管理解题赛 (CTF) 模式题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/ctf")
@RequiredArgsConstructor
public class AdminCtfChallengeController {

    private final AdminCtfChallengeService challengeService;

    @Operation(summary = "分页获取 CTF 题目列表")
    @GetMapping
    public ApiResponse<PageResponse<CtfChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取题目列表成功", challengeService.listChallenges(page, size, categoryId, keyword));
    }

    @Operation(summary = "获取指定 CTF 题目详情")
    @GetMapping("/{id}")
    public ApiResponse<CtfChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取题目详情成功", challengeService.getChallenge(id));
    }

    @Operation(summary = "创建 CTF 题目")
    @Syslog(module = "CHALLENGE", value = "创建 CTF 题目", recordParams = true)
    @PostMapping
    public ApiResponse<Long> createChallenge(
            @Valid @RequestBody CtfChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建题目成功", challengeService.createChallenge(request, authorId));
    }

    @Operation(summary = "更新 CTF 题目")
    @Syslog(module = "CHALLENGE", value = "更新 CTF 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody CtfChallengeUpdateRequestDTO request) {
        challengeService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新题目成功");
    }

    @Operation(summary = "删除 CTF 题目")
    @Syslog(module = "CHALLENGE", value = "删除 CTF 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ApiResponseEntity.success("删除题目成功");
    }

}
