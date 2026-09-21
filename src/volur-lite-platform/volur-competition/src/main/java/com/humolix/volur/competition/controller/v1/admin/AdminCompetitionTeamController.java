package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.AdminTeamUpdateDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamAuditDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamDeleteDTO;
import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.competition.service.admin.AdminTeamService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端参赛队伍审核", description = "管理员审核参赛队伍资质、查看队伍详情与移除队伍接口")
@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/teams")
@RequiredArgsConstructor
public class AdminCompetitionTeamController {

    private final AdminTeamService teamService;

    @Operation(summary = "分页查询参赛队伍列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminTeamListVO>> listTeams(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TeamStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取报名队伍列表成功", teamService.listTeams(competitionId, page, size, status, keyword));
    }

    @Operation(summary = "获取指定参赛队伍详情")
    @GetMapping("/{teamId}")
    public ApiResponse<AdminTeamDetailVO> getTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId) {
        return ApiResponseEntity.ok("获取队伍详情成功", teamService.getTeam(teamId));
    }

    @Operation(summary = "审核参赛队伍资质（通过/驳回）")
    @Syslog(module = "COMPETITION", value = "审核参赛队伍资质", recordParams = true)
    @PostMapping("/{teamId}/audit")
    public ApiResponse<Void> auditTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamAuditRequestDTO request) {
        teamService.auditTeam(teamId, request);
        return ApiResponseEntity.success("队伍资质审核成功");
    }

    @Operation(summary = "管理员更新参赛队伍基本信息")
    @Syslog(module = "COMPETITION", value = "管理员更新队伍信息", recordParams = true)
    @PutMapping("/{teamId}")
    public ApiResponse<Void> updateTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId,
            @Valid @RequestBody AdminTeamUpdateDTO request) {
        teamService.updateTeam(teamId, request);
        return ApiResponseEntity.success("修改队伍信息成功");
    }

    @Operation(summary = "批量审核参赛队伍资质（通过/驳回）")
    @Syslog(module = "COMPETITION", value = "批量审核参赛队伍资质", recordParams = true)
    @PostMapping("/batch-audit")
    public ApiResponse<Void> batchAuditTeams(
            @PathVariable Long competitionId,
            @Valid @RequestBody BatchTeamAuditDTO request) {
        teamService.batchAuditTeams(competitionId, request);
        return ApiResponseEntity.success("批量审核队伍成功");
    }

    @Operation(summary = "批量删除参赛队伍")
    @Syslog(module = "COMPETITION", value = "批量删除参赛队伍", recordParams = true)
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDeleteTeams(
            @PathVariable Long competitionId,
            @Valid @RequestBody BatchTeamDeleteDTO request) {
        teamService.batchDeleteTeams(competitionId, request.getTeamIds());
        return ApiResponseEntity.success("批量删除队伍成功");
    }

    @Operation(summary = "删除参赛队伍")
    @Syslog(module = "COMPETITION", value = "删除参赛队伍", recordParams = true)
    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(
            @PathVariable Long competitionId,
            @PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ApiResponseEntity.success("删除参赛队伍成功");
    }

}
