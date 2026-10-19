package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.AdminTeamUpdateDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamAuditDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamDeleteDTO;
import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.competition.domain.vo.AdminTeamStatsVO;
import com.humolix.volur.competition.service.admin.AdminTeamService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/teams")
@RequiredArgsConstructor
public class AdminCompetitionTeamController {

    private final AdminTeamService teamService;

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminTeamListVO>> listTeams(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TeamStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取报名队伍列表成功", teamService.listTeams(competitionId, page, size, status, keyword));
    }

    @GetMapping("/stats")
    public ApiResponseEntity<AdminTeamStatsVO> getTeamStats(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取报名队伍统计数据成功", teamService.getTeamStats(competitionId));
    }

    @GetMapping("/{teamId}")
    public ApiResponseEntity<AdminTeamDetailVO> getTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId) {
        return ApiResponseEntity.ok("获取队伍详情成功", teamService.getTeam(teamId));
    }

    @Syslog(module = "COMPETITION", value = "审核参赛队伍资质", recordParams = true)
    @PostMapping("/{teamId}/audit")
    public ApiResponseEntity<Void> auditTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamAuditRequestDTO request) {
        teamService.auditTeam(teamId, request);
        return ApiResponseEntity.success("队伍资质审核成功");
    }

    @Syslog(module = "COMPETITION", value = "管理员更新队伍信息", recordParams = true)
    @PutMapping("/{teamId}")
    public ApiResponseEntity<Void> updateTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @Valid @RequestBody AdminTeamUpdateDTO request) {
        teamService.updateTeam(teamId, request);
        return ApiResponseEntity.success("修改队伍信息成功");
    }

    @Syslog(module = "COMPETITION", value = "批量审核参赛队伍资质", recordParams = true)
    @PostMapping("/batch-audit")
    public ApiResponseEntity<Void> batchAuditTeams(
            @PathVariable Long competitionId,
            @Valid @RequestBody BatchTeamAuditDTO request) {
        teamService.batchAuditTeams(competitionId, request);
        return ApiResponseEntity.success("批量审核队伍成功");
    }

    @Syslog(module = "COMPETITION", value = "批量删除参赛队伍", recordParams = true)
    @PostMapping("/batch-delete")
    public ApiResponseEntity<Void> batchDeleteTeams(
            @PathVariable Long competitionId,
            @Valid @RequestBody BatchTeamDeleteDTO request) {
        teamService.batchDeleteTeams(competitionId, request.getTeamIds());
        return ApiResponseEntity.success("批量删除队伍成功");
    }

    @Syslog(module = "COMPETITION", value = "删除参赛队伍", recordParams = true)
    @DeleteMapping("/{teamId}")
    public ApiResponseEntity<Void> deleteTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ApiResponseEntity.success("删除参赛队伍成功");
    }

}
