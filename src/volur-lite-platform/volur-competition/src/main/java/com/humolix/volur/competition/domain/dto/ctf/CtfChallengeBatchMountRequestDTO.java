package com.humolix.volur.competition.domain.dto.ctf;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeBatchMountRequestDTO {

    @NotEmpty(message = "题目挂载列表不能为空")
    private List<CtfChallengeMountRequestDTO> items;

}
