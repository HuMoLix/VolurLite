package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练题分类聚合（含当前用户解出数），用于训练页的分类筛选。
 * id 为题库分类 ID；未归类题目的 id 为 null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingCategoryVO {

    private Long id;

    private String name;

    private long total;

    private long solved;

}
