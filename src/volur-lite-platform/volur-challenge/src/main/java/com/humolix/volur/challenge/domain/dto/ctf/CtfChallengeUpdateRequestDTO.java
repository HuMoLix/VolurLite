package com.humolix.volur.challenge.domain.dto.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeUpdateRequestDTO {

    private String name;

    private String description;

    private Long categoryId;

    private ChallengeType type;

    private ChallengeDifficulty difficulty;

    private String flag;

    private String attachmentUrl;

    private String attachmentName;

    private java.util.List<CtfAttachmentDTO> attachments;

    private String imageName;

    private Integer containerPort;

    private Integer cpuLimit;

    private Integer memoryLimit;

}
