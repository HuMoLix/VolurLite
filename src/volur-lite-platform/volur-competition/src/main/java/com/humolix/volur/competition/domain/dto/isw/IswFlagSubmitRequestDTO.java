package com.humolix.volur.competition.domain.dto.isw;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswFlagSubmitRequestDTO {

    @NotNull(message = "场景题目ID不能为空")
    private Long challengeId;

    private Integer stageIndex;

    @NotBlank(message = "Flag 不能为空")
    private String flag;

}
