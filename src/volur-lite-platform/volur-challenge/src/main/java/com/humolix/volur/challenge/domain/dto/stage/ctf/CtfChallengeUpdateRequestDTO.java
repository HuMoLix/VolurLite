package com.humolix.volur.challenge.domain.dto.stage.ctf;

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

    private Integer difficulty;

    private String flag;

    private String attachmentUrl;

    private String attachmentName;

    private String imageName;

    private Integer containerPort;

    private Integer cpuLimit;

    private Integer memoryLimit;

}
