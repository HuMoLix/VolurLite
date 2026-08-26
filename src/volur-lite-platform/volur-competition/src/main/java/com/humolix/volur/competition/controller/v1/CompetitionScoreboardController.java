package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.vo.scoreboard.CompositeScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfStageScoreboardVO;
import com.humolix.volur.competition.service.CompetitionScoreboardService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "竞赛计分板与排行榜", description = "比赛综合总榜与各阶段分项排行榜查询接口")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/scoreboard")
@RequiredArgsConstructor
public class CompetitionScoreboardController {

    private final CompetitionScoreboardService scoreboardService;

    @Operation(summary = "获取竞赛综合总分排行榜")
    @GetMapping("/composite")
    public ApiResponse<CompositeScoreboardVO> getCompositeScoreboard(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取综合天梯榜成功", scoreboardService.getCompositeScoreboard(competitionId));
    }

    @Operation(summary = "获取 CTF 解题赛分项排行榜")
    @GetMapping("/ctf")
    public ApiResponse<CtfStageScoreboardVO> getCtfStageScoreboard(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取 CTF 解题赛榜单成功", scoreboardService.getCtfStageScoreboard(competitionId));
    }

}
