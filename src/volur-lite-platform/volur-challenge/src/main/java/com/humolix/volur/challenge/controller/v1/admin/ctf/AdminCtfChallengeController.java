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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "管理端 CTF 题库管理", description = "管理员管理解题赛 (CTF) 模式题目增删改查接口")
@RestController
@RequestMapping("/api/v1/admin/challenges/ctf")
@RequiredArgsConstructor
public class AdminCtfChallengeController {

    private final AdminCtfChallengeService challengeService;

    @Operation(summary = "分页获取 CTF 题目列表（仅支持按题目名称模糊检索）")
    @GetMapping
    public ApiResponse<PageResponse<CtfChallengeListVO>> listChallenges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword) {
        String searchName = StringUtils.hasText(name) ? name : keyword;
        return ApiResponseEntity.ok("获取题目列表成功", challengeService.listChallenges(page, size, categoryId, searchName));
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

    @Operation(summary = "导入 CTF 题目 (支持单个题目 ZIP 压缩包，含 build.json/challenge.json 与附件)")
    @Syslog(module = "CHALLENGE", value = "导入 CTF 题目", recordParams = false)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChallengeImportResultVO> importChallenge(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.ok("题目导入处理完成", challengeService.importChallenge(file, authorId));
    }

    @Operation(summary = "下载标准 CTF 题目包模版 (ZIP 压缩包)")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        challengeService.downloadTemplate(response);
    }

    @Operation(summary = "批量导出 CTF 题目 (ZIP 压缩包)")
    @GetMapping("/export")
    public void exportChallenges(
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        challengeService.exportChallenges(ids, response);
    }

    @Operation(summary = "上传 CTF 赛题附件")
    @PostMapping(value = "/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO> uploadAttachment(
            @RequestParam("file") MultipartFile file) {
        return ApiResponseEntity.ok("附件上传成功", challengeService.uploadAttachment(file));
    }

}
