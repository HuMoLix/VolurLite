package com.humolix.volur.competition.controller.v1.ctf;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfTeamSolveRecordVO;
import com.humolix.volur.competition.service.ctf.CtfCompetitionService;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/ctf")
@RequiredArgsConstructor
public class CtfCompetitionController {

    private final CtfCompetitionService ctfCompetitionService;

    @GetMapping("/challenges")
    public ApiResponseEntity<List<CtfCompetitionChallengeVO>> listChallenges(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取 CTF 赛题列表成功", ctfCompetitionService.listChallenges(competitionId, userId));
    }

    @GetMapping("/challenges/{challengeId}")
    public ApiResponseEntity<CtfCompetitionChallengeDetailVO> getChallengeDetail(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取赛题详情成功", ctfCompetitionService.getChallengeDetail(competitionId, challengeId, userId));
    }

    @CompetitionLog(logType = "FLAG", action = "提交 CTF 赛题 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/challenges/{challengeId}/submit")
    public ApiResponseEntity<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId,
            @Valid @RequestBody CtfFlagSubmitRequestDTO request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        boolean isCorrect = ctfCompetitionService.submitFlag(competitionId, challengeId, userId, request, ip);
        return isCorrect
                ? ApiResponseEntity.ok("恭喜！Flag 正确", true)
                : ApiResponseEntity.ok("Flag 错误，请再接再厉", false);
    }

    @CompetitionLog(logType = "CONTAINER", action = "竞赛启动靶机容器", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/start")
    public ApiResponseEntity<ChallengeInstanceResponseVO> startInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("容器启动成功", ctfCompetitionService.startInstance(competitionId, challengeId, userId));
    }

    @CompetitionLog(logType = "CONTAINER", action = "竞赛销毁靶机容器", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/stop")
    public ApiResponseEntity<Void> stopInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        ctfCompetitionService.stopInstance(competitionId, challengeId, userId);
        return ApiResponseEntity.success("容器销毁成功");
    }

    @CompetitionLog(logType = "CONTAINER", action = "竞赛延长靶机容器租期", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/extend")
    public ApiResponseEntity<ChallengeInstanceResponseVO> extendInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("容器延期成功", ctfCompetitionService.extendInstance(competitionId, challengeId, userId));
    }

    @GetMapping("/challenges/{challengeId}/instance")
    public ApiResponseEntity<ChallengeInstanceResponseVO> getInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取靶机容器状态成功", ctfCompetitionService.getInstance(competitionId, challengeId, userId));
    }

    @GetMapping("/teams/{teamId}/solves")
    public ApiResponseEntity<List<CtfTeamSolveRecordVO>> getTeamSolves(
            @PathVariable Long competitionId,
            @PathVariable Long teamId) {
        return ApiResponseEntity.ok("获取队伍解题记录成功", ctfCompetitionService.getTeamSolves(competitionId, teamId));
    }

}
