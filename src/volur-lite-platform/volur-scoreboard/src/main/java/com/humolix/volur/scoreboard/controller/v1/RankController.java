package com.humolix.volur.scoreboard.controller.v1;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.scoreboard.service.RankService;
import com.humolix.volur.scoreboard.vo.RankResponseVO;
import com.humolix.volur.scoreboard.vo.UserRankResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rank")
public class RankController {

    private final RankService rankService;

    @GetMapping("/users")
    public ApiResponse<Page<RankResponseVO<UserRankResponseVO>>> pageUserRanking(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<RankResponseVO<UserRankResponseVO>> res = rankService.pageUserRanking(keyword, page, size);
        return ApiResponseEntity.ok("获取用户排行榜成功", res);
    }

    @GetMapping("/users/me")
    public ApiResponse<RankResponseVO<UserRankResponseVO>> getMyUserRank() {
        Long userId = SecurityUtils.getCurrentUserId();
        RankResponseVO<UserRankResponseVO> res = rankService.getMyUserRank(userId);
        return ApiResponseEntity.ok("获取我的排名成功", res);
    }

}
