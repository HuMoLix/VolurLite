package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingChallengeDetailVO {

    private Long id;

    private Long sourceChallengeId;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String difficulty;

    private Integer points;

    private long solvedCount;

    private boolean solvedByMe;

    /** 当前用户解出时间（未解出为 null） */
    private Instant solvedAt;

    /** Markdown，前端渲染 */
    private String description;

    private List<TrainingAttachmentVO> attachments;

    private boolean dynamicContainer;

}
