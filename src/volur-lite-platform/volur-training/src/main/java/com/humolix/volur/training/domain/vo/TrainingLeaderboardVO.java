package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 训练排行榜：积分 desc → 解出数 desc → 最后解出时间早者优先；
 * 设置了 isHidden 的用户不出现在榜单，但本人仍能看到自己的名次。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingLeaderboardVO {

    private List<Item> items;

    /** 当前用户名次信息（榜外也返回） */
    private MyRank myRank;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        private long rank;

        private Long userId;

        private String username;

        private String avatar;

        private long points;

        private long solvedCount;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyRank {

        private long rank;

        private long points;

        private long solvedCount;

    }

}
