package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.domain.vo.TeamInvitationVO;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/teams")
@RequiredArgsConstructor
public class CompetitionTeamController {

    private final CompetitionTeamService teamService;

    @CompetitionLog(logType = "TEAM", action = "创建队伍并报名竞赛", recordParams = true)
    @PostMapping("/signup")
    public ApiResponseEntity<Long> signupTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TeamSignupRequestDTO request) {
        return ApiResponseEntity.created("创建队伍报名成功", teamService.signupTeam(competitionId, userId, request));
    }

    @GetMapping("/me")
    public ApiResponseEntity<MyTeamInfoVO> getMyTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取我的队伍信息成功", teamService.getMyTeam(competitionId, userId));
    }

    @GetMapping("/{teamId}")
    public ApiResponseEntity<MyTeamInfoVO> getTeamById(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取队伍信息成功", teamService.getTeamById(competitionId, teamId, userId));
    }

    @CompetitionLog(logType = "TEAM", action = "更新参赛队伍信息", recordParams = true)
    @PutMapping("/me")
    public ApiResponseEntity<Void> updateTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TeamUpdateRequestDTO request) {
        teamService.updateTeam(competitionId, userId, request);
        return ApiResponseEntity.success("修改队伍信息成功");
    }

    @CompetitionLog(logType = "TEAM", action = "解散参赛队伍", recordParams = true)
    @DeleteMapping("/me")
    public ApiResponseEntity<Void> deleteMyTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        teamService.deleteMyTeam(competitionId, userId);
        return ApiResponseEntity.success("解散队伍成功");
    }

    @GetMapping("/invitations/me")
    public ApiResponseEntity<List<TeamInvitationVO>> getMyInvitations(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取队伍邀请列表成功", teamService.getMyInvitations(competitionId, userId));
    }

    @CompetitionLog(logType = "TEAM", action = "接受竞赛组队邀请", recordParams = true)
    @PostMapping("/{teamId}/invite/accept")
    public ApiResponseEntity<Void> acceptInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long userId,
            @RequestBody(required = false) TeamInviteAcceptDTO request) {
        teamService.acceptInvitation(competitionId, teamId, userId, request);
        return ApiResponseEntity.success("加入队伍成功");
    }

    @CompetitionLog(logType = "TEAM", action = "拒绝竞赛组队邀请", recordParams = true)
    @PostMapping("/{teamId}/invite/reject")
    public ApiResponseEntity<Void> rejectInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long userId) {
        teamService.rejectInvitation(competitionId, teamId, userId);
        return ApiResponseEntity.success("已拒绝队伍邀请");
    }

    @CompetitionLog(logType = "TEAM", action = "取消队员组队邀请", recordParams = true)
    @DeleteMapping("/{teamId}/invite/cancel")
    public ApiResponseEntity<Void> cancelInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long captainUserId,
            @RequestParam("userId") Long targetUserId) {
        teamService.cancelInvitation(competitionId, teamId, captainUserId, targetUserId);
        return ApiResponseEntity.success("已取消该成员的邀请");
    }

    @CompetitionLog(logType = "TEAM", action = "重新邀请参赛队员", recordParams = true)
    @PostMapping("/{teamId}/invite/reinvite")
    public ApiResponseEntity<Void> reinviteMember(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long captainUserId,
            @RequestParam("userId") Long targetUserId) {
        teamService.reinviteMember(competitionId, teamId, captainUserId, targetUserId);
        return ApiResponseEntity.success("已成功重新发送邀请");
    }

}
