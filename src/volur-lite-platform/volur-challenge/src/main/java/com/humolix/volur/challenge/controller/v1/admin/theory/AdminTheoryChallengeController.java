package com.humolix.volur.challenge.controller.v1.admin.theory;

import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/challenges/theory")
@RequiredArgsConstructor
public class AdminTheoryChallengeController {

    private final AdminTheoryChallengeService theoryService;

    @GetMapping
    public ApiResponseEntity<PageResponse<TheoryChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TheoryType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取理论题目列表成功", theoryService.listChallenges(page, size, type, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<TheoryChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取理论题目详情成功", theoryService.getChallenge(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建理论题目", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createChallenge(
            @Valid @RequestBody TheoryChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建理论题目成功", theoryService.createChallenge(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新理论题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody TheoryChallengeUpdateRequestDTO request) {
        theoryService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新理论题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除理论题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        theoryService.deleteChallenge(id);
        return ApiResponseEntity.success("删除理论题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "导入理论题目", recordParams = false)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseEntity<ChallengeImportResultVO> importChallenges(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.ok("理论题目导入处理完成", theoryService.importChallenges(file, authorId));
    }

    @GetMapping("/export")
    public void exportChallenges(
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        theoryService.exportChallenges(ids, response);
    }

}
