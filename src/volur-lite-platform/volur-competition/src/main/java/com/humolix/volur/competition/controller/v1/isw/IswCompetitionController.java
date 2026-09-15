package com.humolix.volur.competition.controller.v1.isw;

import com.humolix.volur.competition.domain.dto.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.isw.IswCompetitionTopologyVO;
import com.humolix.volur.competition.service.isw.IswCompetitionService;
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

@Tag(name = "竞赛阶段：ISW 实战靶场模式", description = "ISW 场景网络拓扑查询与靶标 Flag 提交接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/isw")
@RequiredArgsConstructor
public class IswCompetitionController {

    private final IswCompetitionService iswCompetitionService;

    @Operation(summary = "获取当前竞赛 ISW 场景拓扑与节点状态")
    @GetMapping("/scenarios")
    public ApiResponse<List<IswCompetitionTopologyVO>> listScenarios(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取综合渗透场景拓扑成功", iswCompetitionService.listScenarios(competitionId, userId));
    }

    @Operation(summary = "提交 ISW 场景靶标 Flag")
    @Syslog(module = "COMPETITION", value = "提交 ISW 靶标 Flag", recordParams = true, recordResponse = true)
    @PostMapping("/submit-flag")
    public ApiResponse<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody IswFlagSubmitRequestDTO request) {
        boolean success = iswCompetitionService.submitFlag(competitionId, userId, request);
        return ApiResponseEntity.ok("提交 Flag 成功", success);
    }

}
