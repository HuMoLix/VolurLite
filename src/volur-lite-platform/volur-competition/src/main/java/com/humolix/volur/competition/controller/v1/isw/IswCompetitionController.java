package com.humolix.volur.competition.controller.v1.isw;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.isw.IswCompetitionTopologyVO;
import com.humolix.volur.competition.service.isw.IswCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/isw")
@RequiredArgsConstructor
public class IswCompetitionController {

    private final IswCompetitionService iswCompetitionService;

    @GetMapping("/scenarios")
    public ApiResponseEntity<List<IswCompetitionTopologyVO>> listScenarios(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取综合渗透场景拓扑成功", iswCompetitionService.listScenarios(competitionId, userId));
    }

    @CompetitionLog(logType = "FLAG", action = "提交 ISW 靶标 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/submit-flag")
    public ApiResponseEntity<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody IswFlagSubmitRequestDTO request) {
        boolean success = iswCompetitionService.submitFlag(competitionId, userId, request);
        return ApiResponseEntity.ok("提交 Flag 成功", success);
    }

}
