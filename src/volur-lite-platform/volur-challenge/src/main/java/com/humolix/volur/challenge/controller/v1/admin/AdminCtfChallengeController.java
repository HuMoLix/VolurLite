package com.humolix.volur.challenge.controller.v1.admin;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeListResponseVO;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/challenges/ctf")
public class AdminCtfChallengeController {

    private final AdminCtfChallengeService challengeService;

    @GetMapping
    public ApiResponse<Page<AdminChallengeListResponseVO>> pageChallenges(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("分页查询题目列表成功", challengeService.pageChallenges(page, size, categoryId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminChallengeDetailResponseVO> getChallengeDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取题目详情成功", challengeService.getChallengeDetail(id));
    }

    @PostMapping
    public ApiResponse<AdminChallengeDetailResponseVO> createChallenge(
            @Valid @RequestBody CtfChallengeCreateRequestDTO request) {
        Long authorId = SecurityUtils.getCurrentUserId();
        return ApiResponseEntity.created("创建题目成功", challengeService.createChallenge(request, authorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminChallengeDetailResponseVO> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody CtfChallengeUpdateRequestDTO request) {
        return ApiResponseEntity.ok("更新题目成功", challengeService.updateChallenge(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ApiResponseEntity.success("删除题目成功");
    }

}
