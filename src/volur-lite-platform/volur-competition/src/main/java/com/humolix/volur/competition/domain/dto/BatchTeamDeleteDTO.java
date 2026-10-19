package com.humolix.volur.competition.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchTeamDeleteDTO {

    @NotEmpty(message = "队伍ID列表不能为空")
    private List<Long> teamIds;

}
