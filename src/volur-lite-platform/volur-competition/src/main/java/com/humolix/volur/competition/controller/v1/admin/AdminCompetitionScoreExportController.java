package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.vo.export.ExportFieldOptionVO;
import com.humolix.volur.competition.service.admin.AdminCompetitionScoreExportService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/scores")
@RequiredArgsConstructor
public class AdminCompetitionScoreExportController {

    private final AdminCompetitionScoreExportService scoreExportService;

    @GetMapping("/export/fields")
    public ApiResponseEntity<List<ExportFieldOptionVO>> getExportFields(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "summary") String scope) {
        return ApiResponseEntity.ok("获取可选导出字段成功", scoreExportService.getExportFieldOptions(competitionId, scope));
    }

    @Syslog(module = "COMPETITION", value = "导出竞赛成绩与报名数据报表", recordParams = true)
    @GetMapping("/export")
    public void exportScores(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(defaultValue = "summary") String scope,
            @RequestParam(required = false) List<String> fields,
            HttpServletResponse response) {
        scoreExportService.exportScores(competitionId, format, scope, fields, response);
    }

}
