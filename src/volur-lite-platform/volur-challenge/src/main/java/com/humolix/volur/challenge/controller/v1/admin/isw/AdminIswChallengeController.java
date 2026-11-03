package com.humolix.volur.challenge.controller.v1.admin.isw;

import com.humolix.volur.challenge.domain.dto.isw.IswChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.isw.IswChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeListVO;
import com.humolix.volur.challenge.service.admin.isw.AdminIswChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/challenges/isw")
@RequiredArgsConstructor
public class AdminIswChallengeController {

    private final AdminIswChallengeService iswService;

    @GetMapping
    public ApiResponseEntity<PageResponse<IswChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取 ISW 题目列表成功", iswService.listChallenges(page, size, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<IswChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取 ISW 题目详情成功", iswService.getChallenge(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建 ISW 题目", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createChallenge(
            @Valid @RequestBody IswChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建 ISW 题目成功", iswService.createChallenge(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新 ISW 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody IswChallengeUpdateRequestDTO request) {
        iswService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新 ISW 题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除 ISW 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        iswService.deleteChallenge(id);
        return ApiResponseEntity.success("删除 ISW 题目成功");
    }

}
