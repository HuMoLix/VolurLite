package com.humolix.volur.scoreboard.service;

import com.humolix.volur.scoreboard.vo.RankResponseVO;
import com.humolix.volur.scoreboard.vo.UserRankResponseVO;
import org.springframework.data.domain.Page;

public interface RankService {

    Page<RankResponseVO<UserRankResponseVO>> pageUserRanking(String keyword, Integer page, Integer size);

    RankResponseVO<UserRankResponseVO> getMyUserRank(Long userId);

}
