package com.humolix.volur.competition.controller.v1.theory;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.theory.TheoryCheatReportDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCheatReportVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.service.theory.TheoryCompetitionService;
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
            @CurrentUserId Long userId) {
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

    @PostMapping("/cheat-event")
    public ApiResponseEntity<TheoryCheatReportVO> reportCheatEvent(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TheoryCheatReportDTO request) {
        return ApiResponseEntity.ok("上报考试违规事件成功", theoryCompetitionService.reportCheatEvent(competitionId, userId, request));
    }

    @PostMapping("/proctor/ready")
    public ApiResponseEntity<Void> registerProctorReady(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        theoryCompetitionService.registerProctorReady(competitionId, userId);
        return ApiResponseEntity.success("反作弊环境检查已完成");
    }

    @PostMapping("/proctor/heartbeat")
    public ApiResponseEntity<com.humolix.volur.competition.domain.vo.theory.TheoryProctorHeartbeatVO> recordProctorHeartbeat(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @RequestBody com.humolix.volur.competition.domain.dto.theory.TheoryProctorHeartbeatDTO request) {
        return ApiResponseEntity.ok("心跳上报成功", theoryCompetitionService.recordProctorHeartbeat(competitionId, userId, request));
    }

    @PostMapping("/proctor/snapshot")
    public ApiResponseEntity<Void> uploadSnapshot(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @RequestBody com.humolix.volur.competition.domain.dto.theory.TheoryProctorSnapshotDTO request) {
        theoryCompetitionService.uploadHighResSnapshot(competitionId, userId, request);
        return ApiResponseEntity.success("高清快照上传成功");
    }

    @PostMapping("/proctor/rtc/signal")
    public ApiResponseEntity<Void> forwardRtcSignal(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @RequestBody com.humolix.volur.competition.domain.dto.theory.TheoryProctorRtcSignalDTO request) {
        theoryCompetitionService.forwardRtcSignal(competitionId, userId, request);
        return ApiResponseEntity.success("WebRTC 信令转发成功");
    }

}
