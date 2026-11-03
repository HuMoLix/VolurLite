package com.humolix.volur.competition.domain.dto.isw;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IswMountRequestDTO {

    @NotNull(message = "题目ID不能为空")
    private Long challengeId;

    private Integer initialPoints;

    private Integer sortOrder;

}
