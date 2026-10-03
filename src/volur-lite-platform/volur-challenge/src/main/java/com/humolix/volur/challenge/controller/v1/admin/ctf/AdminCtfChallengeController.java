package com.humolix.volur.challenge.controller.v1.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfChallengeService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/challenges/ctf")
@RequiredArgsConstructor
public class AdminCtfChallengeController {

    private final AdminCtfChallengeService challengeService;

    @GetMapping
    public ApiResponseEntity<PageResponse<CtfChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword) {
        String searchName = StringUtils.hasText(name) ? name : keyword;
        return ApiResponseEntity.ok("获取题目列表成功", challengeService.listChallenges(page, size, categoryId, searchName));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<CtfChallengeDetailVO> getChallenge(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取题目详情成功", challengeService.getChallenge(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建 CTF 题目", recordParams = true)
    @PostMapping
    public ApiResponseEntity<Long> createChallenge(
            @Valid @RequestBody CtfChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建题目成功", challengeService.createChallenge(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新 CTF 题目", recordParams = true)
    @PutMapping("/{id}")
    public ApiResponseEntity<Void> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody CtfChallengeUpdateRequestDTO request) {
        challengeService.updateChallenge(id, request);
        return ApiResponseEntity.success("更新题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除 CTF 题目", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ApiResponseEntity.success("删除题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "导入 CTF 题目", recordParams = false)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseEntity<ChallengeImportResultVO> importChallenge(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.ok("题目导入处理完成", challengeService.importChallenge(file, authorId));
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        challengeService.downloadTemplate(response);
    }

    @GetMapping("/export")
    public void exportChallenges(
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        challengeService.exportChallenges(ids, response);
    }

    @PostMapping(value = "/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseEntity<com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO> uploadAttachment(
            @RequestParam("file") MultipartFile file) {
        return ApiResponseEntity.ok("附件上传成功", challengeService.uploadAttachment(file));
    }

}
