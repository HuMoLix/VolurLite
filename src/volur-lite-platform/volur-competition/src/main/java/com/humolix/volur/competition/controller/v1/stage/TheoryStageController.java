package com.humolix.volur.competition.controller.v1.stage;

import com.humolix.volur.competition.domain.dto.stage.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.service.stage.theory.TheoryStageService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "竞赛阶段：理论考核模式", description = "理论试卷拉取、在线答题与自动判分交卷接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/stage/theory")
@RequiredArgsConstructor
public class TheoryStageController {

    private final TheoryStageService theoryStageService;

    @Operation(summary = "获取当前用户的理论答卷（题目及选项）")
    @GetMapping("/paper")
    public ApiResponse<TheoryPaperExamVO> getPaper(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取理论试卷成功", theoryStageService.getPaper(competitionId, userId));
    }

    @Operation(summary = "提交理论试卷并获取判分结果")
    @Syslog(module = "COMPETITION", value = "提交理论考核试卷", recordParams = true, recordResponse = true)
    @PostMapping("/submit")
    public ApiResponse<TheoryPaperResultVO> submitPaper(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TheoryPaperSubmitRequestDTO request) {
        return ApiResponseEntity.ok("提交试卷并判分成功", theoryStageService.submitPaper(competitionId, userId, request));
    }

}
