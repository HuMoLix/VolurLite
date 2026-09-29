package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.AdminCompetitionLogQueryDTO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogStatisticsVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogVO;
import com.humolix.volur.competition.service.admin.AdminCompetitionLogService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端比赛运行日志", description = "管理员查看单场比赛容器创建、Flag 提交等核心操作流水与审计接口")
@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompetitionLogController {

    private final AdminCompetitionLogService logService;

    @Operation(summary = "分页查询单场比赛操作日志列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminCompetitionLogVO>> listLogs(
            @PathVariable Long competitionId,
            AdminCompetitionLogQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取比赛日志成功", logService.listLogs(competitionId, queryDTO));
    }

    @Operation(summary = "获取单场比赛日志统计指标快照")
    @GetMapping("/stats")
    public ApiResponse<AdminCompetitionLogStatisticsVO> getStatistics(
            @PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取比赛日志统计指标成功", logService.getStatistics(competitionId));
    }

    @Operation(summary = "获取指定比赛日志详细记录")
    @GetMapping("/{id}")
    public ApiResponse<AdminCompetitionLogVO> getLogDetail(
            @PathVariable Long competitionId,
            @PathVariable Long id) {
        return ApiResponseEntity.ok("获取比赛日志详情成功", logService.getLogDetail(competitionId, id));
    }

}
