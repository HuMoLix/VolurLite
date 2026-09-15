package com.humolix.volur.challenge.controller.v1.admin.theory;

import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
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

@Tag(name = "管理端理论题库管理", description = "管理员管理理论知识（单选/多选/填空/判断）题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/theory")
@RequiredArgsConstructor
public class AdminTheoryChallengeController {

    private final AdminTheoryChallengeService theoryService;

    @Operation(summary = "分页获取理论题目列表")
    @GetMapping
    public ApiResponse<PageResponse<TheoryChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TheoryType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取理论题目列表成功", theoryService.listChallenges(page, size, type, category, keyword));
    }

    @Operation(summary = "获取指定理论题目详情")
    @GetMapping("/{id}")
    public ApiResponse<TheoryChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取理论题目详情成功", theoryService.getChallenge(id));
    }

    @Operation(summary = "创建理论题目")
    @Syslog(module = "CHALLENGE", value = "创建理论题目", recordParams = true)
    @PostMapping
    public ApiResponse<Long> createChallenge(
            @Valid @RequestBody TheoryChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建理论题目成功", theoryService.createChallenge(request, authorId));
    }

    @Operation(summary = "更新理论题目")
    @Syslog(module = "CHALLENGE", value = "更新理论题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody TheoryChallengeUpdateRequestDTO request) {
        theoryService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新理论题目成功");
    }

    @Operation(summary = "删除理论题目")
    @Syslog(module = "CHALLENGE", value = "删除理论题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        theoryService.deleteChallenge(id);
        return ApiResponseEntity.success("删除理论题目成功");
    }

}
