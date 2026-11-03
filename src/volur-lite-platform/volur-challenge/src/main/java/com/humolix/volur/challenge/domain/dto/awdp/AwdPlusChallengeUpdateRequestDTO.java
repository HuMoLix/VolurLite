package com.humolix.volur.challenge.domain.dto.awdp;

import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdPlusChallengeUpdateRequestDTO {

    @Trimmed
    private String name;

    private String description;

    @Trimmed
    private String category;

    private Integer difficulty;

    @Trimmed
    private String sourceCodeUrl;

    @Trimmed
    private String imageName;

    private Integer containerPort;

    private String patchVerifyScript;

    private String exploitVerifyScript;

}
