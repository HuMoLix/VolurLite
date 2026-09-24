package com.humolix.volur.challenge.domain.dto.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeUpdateRequestDTO {

    @Trimmed
    private String name;

    private String description;

    private Long categoryId;

    private ChallengeType type;

    private ChallengeDifficulty difficulty;

    @Trimmed
    private String flag;

    @Trimmed
    private String attachmentUrl;

    @Trimmed
    private String attachmentName;

    private java.util.List<CtfAttachmentDTO> attachments;

    @Trimmed
    private String imageName;

    private Integer containerPort;

    private Integer cpuLimit;

    private Integer memoryLimit;

}
