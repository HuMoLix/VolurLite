package com.humolix.volur.challenge.controller.v1.admin.stage.isw;

import com.humolix.volur.challenge.domain.dto.stage.isw.IswChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.isw.IswChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeListVO;
import com.humolix.volur.challenge.service.admin.stage.isw.AdminIswChallengeService;
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

@Tag(name = "管理端 ISW 题库管理", description = "管理员管理信息安全工控/实战 (ISW) 模式题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/isw")
@RequiredArgsConstructor
public class AdminIswChallengeController {

    private final AdminIswChallengeService iswService;

    @Operation(summary = "分页获取 ISW 题目列表")
    @GetMapping
    public ApiResponse<PageResponse<IswChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取 ISW 题目列表成功", iswService.listChallenges(page, size, category, keyword));
    }

    @Operation(summary = "获取指定 ISW 题目详情")
    @GetMapping("/{id}")
    public ApiResponse<IswChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取 ISW 题目详情成功", iswService.getChallenge(id));
    }

    @Operation(summary = "创建 ISW 题目")
    @Syslog(module = "CHALLENGE", value = "创建 ISW 题目", recordParams = true)
    @PostMapping
    public ApiResponse<Long> createChallenge(
            @Valid @RequestBody IswChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建 ISW 题目成功", iswService.createChallenge(request, authorId));
    }

    @Operation(summary = "更新 ISW 题目")
    @Syslog(module = "CHALLENGE", value = "更新 ISW 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody IswChallengeUpdateRequestDTO request) {
        iswService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新 ISW 题目成功");
    }

    @Operation(summary = "删除 ISW 题目")
    @Syslog(module = "CHALLENGE", value = "删除 ISW 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        iswService.deleteChallenge(id);
        return ApiResponseEntity.success("删除 ISW 题目成功");
    }

}
