package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryMountRequestDTO {

    @NotNull(message = "题目ID不能为空")
    private Long challengeId;

    private Integer score;

    private Integer sortOrder;

}
