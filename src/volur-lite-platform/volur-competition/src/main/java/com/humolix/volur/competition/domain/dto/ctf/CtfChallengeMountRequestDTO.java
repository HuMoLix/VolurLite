package com.humolix.volur.competition.domain.dto.ctf;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeMountRequestDTO {

    @NotNull(message = "题库题目ID不能为空")
    private Long challengeId;

    private Integer initialPoints;

    private Integer minPoints;

    private Integer decayCount;

    private Integer sortOrder;

    private Boolean isVisible;

}
