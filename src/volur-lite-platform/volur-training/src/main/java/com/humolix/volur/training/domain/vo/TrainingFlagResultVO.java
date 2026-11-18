package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * flag 提交结果三态：CORRECT 首次解出得分 / DUPLICATE 已解出过 / WRONG 错误
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingFlagResultVO {

    public enum Result { CORRECT, DUPLICATE, WRONG }

    private Result result;

    /** 本次得分，仅 CORRECT 时大于 0 */
    private int pointsEarned;

    /** 用户当前训练总积分 */
    private long totalPoints;

}
