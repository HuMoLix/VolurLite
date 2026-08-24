package com.humolix.volur.challenge.object.dto.stage.ctf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfTrainingChallengeUpdateRequestDTO {

    private Integer initialPoints;
    private Integer minimumPoints;
    private Boolean isDynamicScore;
    private Float decayCoefficient;
    private Boolean isActive;
    private Integer cpuLimit;
    private Integer memoryLimit;

}
