package com.humolix.volur.competition.controller.v1;

import com.humolix.volur.competition.domain.vo.scoreboard.CompositeScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfScoreboardVO;
import com.humolix.volur.competition.service.CompetitionScoreboardService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/scoreboard")
@RequiredArgsConstructor
public class CompetitionScoreboardController {

    private final CompetitionScoreboardService scoreboardService;

    @GetMapping({"", "/composite"})
    public ApiResponseEntity<CompositeScoreboardVO> getCompositeScoreboard(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取竞赛天梯榜成功", scoreboardService.getCompositeScoreboard(competitionId));
    }

    @GetMapping("/ctf")
    public ApiResponseEntity<CtfScoreboardVO> getCtfScoreboard(@PathVariable Long competitionId) {
        return ApiResponseEntity.ok("获取 CTF 解题赛榜单成功", scoreboardService.getCtfScoreboard(competitionId));
    }

}
