package com.humolix.volur.challenge.domain.dto.stage.awd;

import com.humolix.volur.challenge.domain.enums.AwdCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdChallengeUpdateRequestDTO {

    private String name;

    private String description;

    private AwdCategory category;

    private Integer difficulty;

    private String attachmentUrl;

    private String imageName;

    private Integer containerPort;

    private String checkScriptUrl;

    private String flagPath;

}
