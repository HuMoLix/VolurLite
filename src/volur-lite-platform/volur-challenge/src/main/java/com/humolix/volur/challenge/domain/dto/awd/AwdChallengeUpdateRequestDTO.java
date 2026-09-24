package com.humolix.volur.challenge.domain.dto.awd;

import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdChallengeUpdateRequestDTO {

    @Trimmed
    private String name;

    private String description;

    private AwdCategory category;

    private Integer difficulty;

    @Trimmed
    private String attachmentUrl;

    @Trimmed
    private String imageName;

    private Integer containerPort;

    @Trimmed
    private String checkScriptUrl;

    @Trimmed
    private String flagPath;

}
