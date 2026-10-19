package com.humolix.volur.challenge.controller.v1.admin.theory;

import com.humolix.volur.challenge.domain.dto.theory.TheoryBankCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryBankUpdateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankListVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
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

    @GetMapping("/banks")
    public ApiResponseEntity<PageResponse<TheoryBankListVO>> listBanks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取题库列表成功", theoryService.listBanks(page, size, keyword));
    }

    @GetMapping("/banks/{id}")
    public ApiResponseEntity<TheoryBankDetailVO> getBank(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取题库详情成功", theoryService.getBank(id));
    }

    @Syslog(module = "CHALLENGE", value = "创建理论题库", recordParams = true)
    @PostMapping("/banks")
    public ApiResponseEntity<Long> createBank(
            @Valid @RequestBody TheoryBankCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.created("创建题库成功", theoryService.createBank(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新理论题库", recordParams = true)
    @PutMapping("/banks/{id}")
    public ApiResponseEntity<Void> updateBank(
            @PathVariable Long id,
            @Valid @RequestBody TheoryBankUpdateRequestDTO request) {
        theoryService.updateBank(id, request);
        return ApiResponseEntity.success("更新题库成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除理论题库", recordParams = true)
    @DeleteMapping("/banks/{id}")
    public ApiResponseEntity<Void> deleteBank(@PathVariable Long id) {
        theoryService.deleteBank(id);
        return ApiResponseEntity.success("删除题库成功");
    }

    @Syslog(module = "CHALLENGE", value = "导入理论题库", recordParams = false)
    @PostMapping(value = "/banks/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseEntity<ChallengeImportResultVO> importBanks(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.ok("题库导入处理完成", theoryService.importBanks(file, authorId));
    }

    @GetMapping("/banks/export")
    public void exportBanks(
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        theoryService.exportBanks(ids, response);
    }

    @GetMapping("/banks/{bankId}/questions")
    public ApiResponseEntity<PageResponse<TheoryChallengeListVO>> listBankQuestions(
            @PathVariable Long bankId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TheoryType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取题库题目列表成功", theoryService.listBankQuestions(bankId, page, size, type, category, keyword));
    }

    @Syslog(module = "CHALLENGE", value = "向题库添加题目", recordParams = true)
    @PostMapping("/banks/{bankId}/questions")
    public ApiResponseEntity<Long> createQuestionInBank(
            @PathVariable Long bankId,
            @Valid @RequestBody TheoryChallengeCreateRequestDTO request,
            @CurrentUserId Long authorId) {
        request.setBankId(bankId);
        return ApiResponseEntity.created("创建题目成功", theoryService.createChallenge(request, authorId));
    }

    @Syslog(module = "CHALLENGE", value = "更新题库中的题目", recordParams = true)
    @PutMapping("/banks/{bankId}/questions/{questionId}")
    public ApiResponseEntity<Void> updateQuestionInBank(
            @PathVariable Long bankId,
            @PathVariable Long questionId,
            @Valid @RequestBody TheoryChallengeUpdateRequestDTO request) {
        request.setBankId(bankId);
        theoryService.updateChallenge(questionId, request);
        return ApiResponseEntity.success("更新题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "删除题库中的题目", recordParams = true)
    @DeleteMapping("/banks/{bankId}/questions/{questionId}")
    public ApiResponseEntity<Void> deleteQuestionInBank(
            @PathVariable Long bankId,
            @PathVariable Long questionId) {
        theoryService.deleteChallenge(questionId);
        return ApiResponseEntity.success("删除题目成功");
    }

    @Syslog(module = "CHALLENGE", value = "导入题目到指定题库", recordParams = false)
    @PostMapping(value = "/banks/{bankId}/questions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseEntity<ChallengeImportResultVO> importQuestionsToBank(
            @PathVariable Long bankId,
            @RequestParam("file") MultipartFile file,
            @CurrentUserId Long authorId) {
        return ApiResponseEntity.ok("题目导入处理完成", theoryService.importQuestionsToBank(bankId, file, authorId));
    }

    @GetMapping("/banks/{bankId}/questions/export")
    public void exportQuestionsFromBank(
            @PathVariable Long bankId,
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        theoryService.exportQuestionsByBank(bankId, ids, response);
    }

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
