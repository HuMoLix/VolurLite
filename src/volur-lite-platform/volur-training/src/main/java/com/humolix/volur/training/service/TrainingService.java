package com.humolix.volur.training.service;

import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.training.domain.vo.*;

import java.util.List;

/**
 * 训练模块用户端服务：题目列表/详情、flag 提交、靶机状态、个人进度、排行榜。
 */
public interface TrainingService {

    PageResponse<TrainingChallengeListVO> listChallenges(int page, int size, Long categoryId, String difficulty, String keyword, Long userId);

    /** 训练池分类聚合（含当前用户解出数），供分类筛选使用 */
    List<TrainingCategoryVO> getCategories(Long userId);

    TrainingChallengeDetailVO getChallenge(Long id, Long userId);

    TrainingFlagResultVO submitFlag(Long id, Long userId, String flag);

    ChallengeInstanceResponseVO getInstance(Long id, Long userId);

    TrainingProgressVO getProgress(Long userId);

    TrainingLeaderboardVO getLeaderboard(int limit, Long userId);

}
