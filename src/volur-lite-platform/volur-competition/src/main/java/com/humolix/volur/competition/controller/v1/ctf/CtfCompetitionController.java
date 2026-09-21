package com.humolix.volur.competition.controller.v1.ctf;

import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.service.ctf.CtfCompetitionService;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.competition.annotation.CompetitionLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛阶段：CTF 解题模式", description = "CTF 解题赛题目列表、题目详情、Flag 提交与动态靶机容器管理接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/ctf")
@RequiredArgsConstructor
public class CtfCompetitionController {

    private final CtfCompetitionService ctfCompetitionService;

    @Operation(summary = "获取竞赛 CTF 题目列表（含解题状态）")
    @GetMapping("/challenges")
    public ApiResponse<List<CtfCompetitionChallengeVO>> listChallenges(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取 CTF 赛题列表成功", ctfCompetitionService.listChallenges(competitionId, userId));
    }

    @Operation(summary = "获取竞赛 CTF 题目详情")
    @GetMapping("/challenges/{challengeId}")
    public ApiResponse<CtfCompetitionChallengeDetailVO> getChallengeDetail(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取赛题详情成功", ctfCompetitionService.getChallengeDetail(competitionId, challengeId, userId));
    }

    @Operation(summary = "提交 CTF 题目 Flag")
    @CompetitionLog(logType = "FLAG", action = "提交 CTF 赛题 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/challenges/{challengeId}/submit")
    public ApiResponse<Boolean> submitFlag(
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

    @Operation(summary = "为比赛题目启动独立靶机容器")
    @CompetitionLog(logType = "CONTAINER", action = "竞赛启动靶机容器", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/start")
    public ApiResponse<ChallengeInstanceResponseVO> startInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("容器启动成功", ctfCompetitionService.startInstance(competitionId, challengeId, userId));
    }

    @Operation(summary = "销毁比赛题目的靶机容器")
    @CompetitionLog(logType = "CONTAINER", action = "竞赛销毁靶机容器", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/stop")
    public ApiResponse<Void> stopInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        ctfCompetitionService.stopInstance(competitionId, challengeId, userId);
        return ApiResponseEntity.success("容器销毁成功");
    }

    @Operation(summary = "延长比赛题目的靶机容器租期")
    @CompetitionLog(logType = "CONTAINER", action = "竞赛延长靶机容器租期", recordParams = true)
    @PostMapping("/challenges/{challengeId}/instance/extend")
    public ApiResponse<ChallengeInstanceResponseVO> extendInstance(
            @PathVariable Long competitionId,
            @PathVariable Long challengeId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("容器延期成功", ctfCompetitionService.extendInstance(competitionId, challengeId, userId));
    }

}
