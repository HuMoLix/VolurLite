package com.humolix.volur.competition.controller.v1.admin.theory;

import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionConfigDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionPaperGenerateRequestDTO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCompetitionConfigVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperQuestionAdminVO;
import com.humolix.volur.competition.domain.vo.theory.AdminTheoryCandidateMonitorVO;
import com.humolix.volur.competition.service.admin.theory.AdminTheoryCompetitionService;
import com.humolix.volur.competition.service.theory.TheoryCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/theory")
@RequiredArgsConstructor
public class AdminTheoryCompetitionController {

    private final AdminTheoryCompetitionService theoryCompetitionService;
    private final TheoryCompetitionService theoryProctorService;

    @GetMapping("/config")
    public ApiResponseEntity<TheoryCompetitionConfigVO> getConfig(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取理论赛制与题库配置成功", theoryCompetitionService.getConfig(competitionId));
    }

    @Syslog(module = "COMPETITION", value = "保存理论赛题规则配置", recordParams = true)
    @PutMapping("/config")
    public ApiResponseEntity<TheoryCompetitionConfigVO> saveConfig(
            @PathVariable Long competitionId,
            @Valid @RequestBody TheoryCompetitionConfigDTO request) {
        return ApiResponseEntity.ok("保存理论赛题配置成功", theoryCompetitionService.saveConfig(competitionId, request));
    }

    @Syslog(module = "COMPETITION", value = "从题库生成竞赛试卷", recordParams = true)
    @PostMapping("/paper/generate")
    public ApiResponseEntity<Integer> generatePaper(
            @PathVariable Long competitionId,
            @Valid @RequestBody TheoryCompetitionPaperGenerateRequestDTO request) {
        int count = theoryCompetitionService.generatePaper(competitionId, request);
        return ApiResponseEntity.ok("试卷生成成功，共生成 " + count + " 道考题", count);
    }

    @GetMapping("/paper")
    public ApiResponseEntity<List<TheoryPaperQuestionAdminVO>> listPaperQuestions(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取竞赛试卷题目列表成功", theoryCompetitionService.listPaperQuestions(competitionId));
    }

    @Syslog(module = "COMPETITION", value = "移除试卷题目", recordParams = true)
    @DeleteMapping("/paper/{paperId}")
    public ApiResponseEntity<Void> removePaperQuestion(
            @PathVariable Long competitionId,
            @PathVariable Long paperId) {
        theoryCompetitionService.removePaperQuestion(competitionId, paperId);
        return ApiResponseEntity.success("移除题目成功");
    }

    @GetMapping("/monitor/candidates")
    public ApiResponseEntity<List<AdminTheoryCandidateMonitorVO>> listCandidateMonitors(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取理论考试在线监考考生列表成功", theoryProctorService.listCandidateMonitors(competitionId));
    }

    @PostMapping("/monitor/candidates/{candidateUserId}/snapshot-request")
    public ApiResponseEntity<Void> requestCandidateSnapshot(
            @PathVariable Long competitionId,
            @PathVariable Long candidateUserId,
            @CurrentUserId Long adminUserId) {
        theoryProctorService.requestCandidateHighResSnapshot(competitionId, adminUserId, candidateUserId);
        return ApiResponseEntity.success("已向考生发送高清快照请求指令");
    }

    @PostMapping("/monitor/rtc/signal")
    public ApiResponseEntity<Void> forwardAdminRtcSignal(
            @PathVariable Long competitionId,
            @CurrentUserId Long adminUserId,
            @RequestBody com.humolix.volur.competition.domain.dto.theory.TheoryProctorRtcSignalDTO request) {
        theoryProctorService.forwardRtcSignal(competitionId, adminUserId, request);
        return ApiResponseEntity.success("WebRTC 信令转发成功");
    }

}
