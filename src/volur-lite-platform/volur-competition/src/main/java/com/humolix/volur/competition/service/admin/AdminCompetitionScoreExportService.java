package com.humolix.volur.competition.service.admin;

import com.humolix.volur.competition.domain.vo.export.ExportFieldOptionVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AdminCompetitionScoreExportService {

    void exportScores(Long competitionId, String format, String scope, List<String> fields, HttpServletResponse response);

    List<ExportFieldOptionVO> getExportFieldOptions(Long competitionId, String scope);

}
