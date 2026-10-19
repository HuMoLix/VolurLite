package com.humolix.volur.challenge.controller.v1.admin.awd;

import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeListVO;
import com.humolix.volur.challenge.service.admin.awd.AdminAwdChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/challenges/awd")
@RequiredArgsConstructor
public class AdminAwdChallengeController {

    private final AdminAwdChallengeService awdService;

    @GetMapping
    public ApiResponseEntity<PageResponse<AwdChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AwdCategory category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取 AWD 题目列表成功", awdService.listChallenges(page, size, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AwdChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取 AWD 题目详情成功", awdService.getChallenge(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建 AWD 题目", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createChallenge(
            @Valid @RequestBody AwdChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建 AWD 题目成功", awdService.createChallenge(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新 AWD 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody AwdChallengeUpdateRequestDTO request) {
        awdService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新 AWD 题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除 AWD 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        awdService.deleteChallenge(id);
        return ApiResponseEntity.success("删除 AWD 题目成功");
    }

}
