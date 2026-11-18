package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingProgressVO {

    private long totalChallenges;

    private long solvedCount;

    private long totalPoints;

    /** 榜外也有名次（= 参与人数 + 1） */
    private long rank;

    private List<CategoryProgress> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryProgress {

        private String category;

        private long total;

        private long solved;

    }

}
