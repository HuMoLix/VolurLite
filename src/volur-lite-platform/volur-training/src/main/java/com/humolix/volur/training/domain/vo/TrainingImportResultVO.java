package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入结果：imported 成功数；skipped 被跳过的题及原因
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingImportResultVO {

    public enum SkipReason { ALREADY_IN_POOL, NOT_FOUND }

    private int imported;

    private List<Skipped> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skipped {

        private Long challengeId;

        private SkipReason reason;

    }

}
