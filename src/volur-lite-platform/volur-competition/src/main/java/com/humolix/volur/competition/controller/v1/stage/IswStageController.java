package com.humolix.volur.competition.controller.v1.stage;

import com.humolix.volur.competition.domain.dto.stage.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.isw.IswCompetitionTopologyVO;
import com.humolix.volur.competition.service.stage.isw.IswStageService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛阶段：ISW 实战靶场模式", description = "ISW 场景网络拓扑查询与靶标 Flag 提交接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/stage/isw")
@RequiredArgsConstructor
public class IswStageController {

    private final IswStageService iswStageService;

    @Operation(summary = "获取当前竞赛 ISW 场景拓扑与节点状态")
    @GetMapping("/scenarios")
    public ApiResponse<List<IswCompetitionTopologyVO>> listScenarios(
            @PathVariable Long competitionId,
            @CurrentUserId(required = false) Long userId) {
        return ApiResponseEntity.ok("获取综合渗透场景拓扑成功", iswStageService.listScenarios(competitionId, userId));
    }

    @Operation(summary = "提交 ISW 场景靶标 Flag")
    @PostMapping("/submit-flag")
    public ApiResponse<Boolean> submitFlag(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId,
            @Valid @RequestBody IswFlagSubmitRequestDTO request) {
        boolean success = iswStageService.submitFlag(competitionId, userId, request);
        return ApiResponseEntity.ok("提交 Flag 成功", success);
    }

}
