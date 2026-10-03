package com.humolix.volur.competition.controller.v1.theory;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.service.theory.TheoryCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/theory")
@RequiredArgsConstructor
public class TheoryCompetitionController {

    private final TheoryCompetitionService theoryCompetitionService;

    @GetMapping("/paper")
    public ApiResponseEntity<TheoryPaperExamVO> getPaper(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取理论试卷成功", theoryCompetitionService.getPaper(competitionId, userId));
    }

    @CompetitionLog(logType = "THEORY", action = "提交理论考核试卷", recordParams = true, recordResponse = true)
    @PostMapping("/submit")
    public ApiResponseEntity<TheoryPaperResultVO> submitPaper(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TheoryPaperSubmitRequestDTO request) {
        return ApiResponseEntity.ok("提交试卷并判分成功", theoryCompetitionService.submitPaper(competitionId, userId, request));
    }

}
