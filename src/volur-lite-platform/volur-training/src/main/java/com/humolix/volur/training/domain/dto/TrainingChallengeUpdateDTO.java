package com.humolix.volur.training.domain.dto;

import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改训练题：字段为 null 表示不修改
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingChallengeUpdateDTO {

    @Min(value = 0, message = "分值不能为负数")
    private Integer points;

    private TrainingChallengeStatus status;

}
