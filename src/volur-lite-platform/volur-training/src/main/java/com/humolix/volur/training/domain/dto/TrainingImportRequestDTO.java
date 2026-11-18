package com.humolix.volur.training.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入：把题库题目加入训练池（已在池中或题库不存在的会被跳过并在结果中说明）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingImportRequestDTO {

    @NotEmpty(message = "题目 ID 列表不能为空")
    private List<Long> challengeIds;

}
