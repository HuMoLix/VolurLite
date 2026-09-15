package com.humolix.volur.challenge.domain.dto.isw;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswChallengeUpdateRequestDTO {

    private String name;

    private String description;

    private String category;

    private Integer difficulty;

    private String topologyJson;

    private String stageFlagsJson;

}
