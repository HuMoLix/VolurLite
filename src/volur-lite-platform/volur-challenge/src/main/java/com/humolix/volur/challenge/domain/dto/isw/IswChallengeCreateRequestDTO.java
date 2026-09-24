package com.humolix.volur.challenge.domain.dto.isw;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    @Trimmed
    private String name;

    private String description;

    @Trimmed
    private String category;

    private Integer difficulty;

    private String topologyJson;

    private String stageFlagsJson;

}
