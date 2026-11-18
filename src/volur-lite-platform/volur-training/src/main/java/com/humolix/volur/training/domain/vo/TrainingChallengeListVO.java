package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练题列表项（用户端/管理端共用字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingChallengeListVO {

    private Long id;

    /** 题库原题 ID：靶机启停接口直接使用它 */
    private Long sourceChallengeId;

    private String name;

    private Long categoryId;

    private String categoryName;

    /** 难度描述值：easy / normal / mid / hard / difficult */
    private String difficulty;

    private Integer points;

    private long solvedCount;

    private boolean solvedByMe;

    private boolean hasAttachment;

    /** 原题是否配置了靶机镜像（决定前端是否显示"启动靶机"） */
    private boolean dynamicContainer;

    /** 管理端可见；用户端列表恒为 ENABLED，不输出 */
    private String status;

}
