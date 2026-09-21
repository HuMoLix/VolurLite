package com.humolix.volur.competition.controller.v1.awd;

import com.humolix.volur.competition.domain.dto.awd.AwdFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.awd.AwdCurrentRoundVO;
import com.humolix.volur.competition.service.awd.AwdCompetitionService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.competition.annotation.CompetitionLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "竞赛阶段：AWD 攻防模式", description = "AWD 攻防对抗阶段轮次信息与攻击 Flag 提交接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/awd")
@RequiredArgsConstructor
public class AwdCompetitionController {

    private final AwdCompetitionService awdCompetitionService;

    @Operation(summary = "获取当前 AWD 攻防轮次与队伍防守状态")
    @GetMapping("/rounds/current")
    public ApiResponse<AwdCurrentRoundVO> getCurrentRound(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取当前攻防轮次信息成功", awdCompetitionService.getCurrentRound(competitionId, userId));
    }

    @Operation(summary = "提交 AWD 攻击 Flag")
    @CompetitionLog(logType = "FLAG", action = "提交 AWD 攻击 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/submit-flag")
    public ApiResponse<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody AwdFlagSubmitRequestDTO request) {
        boolean success = awdCompetitionService.submitFlag(competitionId, userId, request);
        return ApiResponseEntity.ok("提交 Flag 成功", success);
    }

}
