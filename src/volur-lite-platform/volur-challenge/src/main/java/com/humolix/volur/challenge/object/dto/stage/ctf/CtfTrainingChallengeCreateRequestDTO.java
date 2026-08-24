package com.humolix.volur.challenge.object.dto.stage.ctf;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfTrainingChallengeCreateRequestDTO {

    @NotNull(message = "题目ID不能为空")
    private Long challengeId;

    @NotNull(message = "初始分值不能为空")
    private Integer initialPoints;

    @NotNull(message = "最低分值不能为空")
    private Integer minimumPoints;

    private Boolean isDynamicScore = false;

    private Float decayCoefficient = 0.5f;

    private Boolean isActive = true;

    private Integer cpuLimit = 1;

    private Integer memoryLimit = 128;

}
