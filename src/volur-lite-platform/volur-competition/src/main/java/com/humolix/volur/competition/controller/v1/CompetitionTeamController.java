package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.vo.CompetitionTeamVO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.domain.vo.TeamInvitationVO;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛战队与报名管理", description = "参赛战队创建、报名、信息修改与成员邀请确认接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/teams")
@RequiredArgsConstructor
public class CompetitionTeamController {

    private final CompetitionTeamService teamService;

    @Operation(summary = "获取当前竞赛所有参赛队伍列表")
    @GetMapping
    public ApiResponse<List<CompetitionTeamVO>> listTeams(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取参赛队伍列表成功", teamService.listTeams(competitionId, userId));
    }

    @Operation(summary = "创建队伍并完成报名")
    @Syslog(module = "COMPETITION", value = "创建队伍并报名竞赛", recordParams = true)
    @PostMapping("/signup")
    public ApiResponse<Long> signupTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TeamSignupRequestDTO request) {
        return ApiResponseEntity.created("创建队伍报名成功", teamService.signupTeam(competitionId, userId, request));
    }

    @Operation(summary = "获取当前用户在该比赛中的队伍详情")
    @GetMapping("/me")
    public ApiResponse<MyTeamInfoVO> getMyTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取我的队伍信息成功", teamService.getMyTeam(competitionId, userId));
    }

    @Operation(summary = "更新我的战队信息（队长权限）")
    @Syslog(module = "COMPETITION", value = "更新参赛队伍信息", recordParams = true)
    @PutMapping("/me")
    public ApiResponse<Void> updateTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody TeamUpdateRequestDTO request) {
        teamService.updateTeam(competitionId, userId, request);
        return ApiResponseEntity.success("修改队伍信息成功");
    }

    @Operation(summary = "解散我的战队（队长权限）")
    @Syslog(module = "COMPETITION", value = "解散参赛队伍", recordParams = true)
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyTeam(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        teamService.deleteMyTeam(competitionId, userId);
        return ApiResponseEntity.success("解散队伍成功");
    }

    @Operation(summary = "获取当前用户收到的本场竞赛战队邀请列表")
    @GetMapping("/invitations/me")
    public ApiResponse<List<TeamInvitationVO>> getMyInvitations(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取队伍邀请列表成功", teamService.getMyInvitations(competitionId, userId));
    }

    @Operation(summary = "受邀队员确认接受组队邀请")
    @Syslog(module = "COMPETITION", value = "接受竞赛组队邀请", recordParams = true)
    @PostMapping("/{teamId}/invite/accept")
    public ApiResponse<Void> acceptInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long userId,
            @RequestBody(required = false) TeamInviteAcceptDTO request) {
        teamService.acceptInvitation(competitionId, teamId, userId, request);
        return ApiResponseEntity.success("加入队伍成功");
    }

    @Operation(summary = "受邀队员拒绝组队邀请")
    @Syslog(module = "COMPETITION", value = "拒绝竞赛组队邀请", recordParams = true)
    @PostMapping("/{teamId}/invite/reject")
    public ApiResponse<Void> rejectInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long userId) {
        teamService.rejectInvitation(competitionId, teamId, userId);
        return ApiResponseEntity.success("已拒绝队伍邀请");
    }

    @Operation(summary = "队长取消已发送的队员组队邀请")
    @Syslog(module = "COMPETITION", value = "取消队员组队邀请", recordParams = true)
    @DeleteMapping("/{teamId}/invite/cancel")
    public ApiResponse<Void> cancelInvitation(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @CurrentUserId Long captainUserId,
            @RequestParam("userId") Long targetUserId) {
        teamService.cancelInvitation(competitionId, teamId, captainUserId, targetUserId);
        return ApiResponseEntity.success("已取消该成员的邀请");
    }

}
