package com.humolix.volur.competition.domain.dto.awd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdFlagSubmitRequestDTO {

    @NotNull(message = "目标题目ID不能为空")
    private Long challengeId;

    @NotNull(message = "目标队伍ID不能为空")
    private Long victimTeamId;

    @NotBlank(message = "Flag 不能为空")
    private String flag;

}
