package com.humolix.volur.challenge.domain.dto.stage.awdp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdPlusChallengeUpdateRequestDTO {

    private String name;

    private String description;

    private String category;

    private Integer difficulty;

    private String sourceCodeUrl;

    private String imageName;

    private Integer containerPort;

    private String patchVerifyScript;

    private String exploitVerifyScript;

}
