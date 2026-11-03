package com.humolix.volur.challenge.domain.vo.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeDetailVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private Long authorId;
    private String authorName;
    private ChallengeType type;
    private ChallengeDifficulty difficulty;
    private String flag;
    private String attachmentUrl;
    private String attachmentName;
    private java.util.List<CtfAttachmentVO> attachments;
    private String imageName;
    private Integer containerPort;
    private Integer cpuLimit;
    private Integer memoryLimit;
    private Instant createdAt;
    private Instant updatedAt;

}
