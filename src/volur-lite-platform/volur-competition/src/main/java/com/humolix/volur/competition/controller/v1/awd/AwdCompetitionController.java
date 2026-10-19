package com.humolix.volur.competition.controller.v1.awd;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.awd.AwdFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.awd.AwdCurrentRoundVO;
import com.humolix.volur.competition.service.awd.AwdCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/awd")
@RequiredArgsConstructor
public class AwdCompetitionController {

    private final AwdCompetitionService awdCompetitionService;

    @GetMapping("/rounds/current")
    public ApiResponseEntity<AwdCurrentRoundVO> getCurrentRound(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取当前攻防轮次信息成功", awdCompetitionService.getCurrentRound(competitionId, userId));
    }

    @CompetitionLog(logType = "FLAG", action = "提交 AWD 攻击 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/submit-flag")
    public ApiResponseEntity<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody AwdFlagSubmitRequestDTO request) {
        boolean success = awdCompetitionService.submitFlag(competitionId, userId, request);
        return ApiResponseEntity.ok("提交 Flag 成功", success);
    }

}
