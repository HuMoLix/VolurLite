package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.AdminCompetitionLogQueryDTO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogStatisticsVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogVO;
import com.humolix.volur.competition.service.admin.AdminCompetitionLogService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompetitionLogController {

    private final AdminCompetitionLogService logService;

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminCompetitionLogVO>> listLogs(
            @PathVariable Long competitionId,
            AdminCompetitionLogQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取比赛日志成功", logService.listLogs(competitionId, queryDTO));
    }

    @GetMapping("/stats")
    public ApiResponseEntity<AdminCompetitionLogStatisticsVO> getStatistics(
            @PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取比赛日志统计指标成功", logService.getStatistics(competitionId));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminCompetitionLogVO> getLogDetail(
            @PathVariable Long competitionId,
            @PathVariable Long id) {
        return ApiResponseEntity.ok("获取比赛日志详情成功", logService.getLogDetail(competitionId, id));
    }

}
