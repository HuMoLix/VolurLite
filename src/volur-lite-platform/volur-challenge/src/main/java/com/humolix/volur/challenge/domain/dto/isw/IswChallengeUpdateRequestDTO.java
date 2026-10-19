package com.humolix.volur.challenge.domain.dto.isw;

import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswChallengeUpdateRequestDTO {

    @Trimmed
    private String name;

    private String description;

    @Trimmed
    private String category;

    private Integer difficulty;

    private String topologyJson;

    private String stageFlagsJson;

}
