package com.humolix.volur.challenge.domain.dto.isw;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    private String name;

    private String description;

    private String category;

    private Integer difficulty;

    private String topologyJson;

    private String stageFlagsJson;

}
