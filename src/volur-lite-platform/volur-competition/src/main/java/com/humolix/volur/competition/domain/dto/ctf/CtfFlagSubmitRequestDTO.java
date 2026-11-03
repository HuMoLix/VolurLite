package com.humolix.volur.competition.domain.dto.ctf;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfFlagSubmitRequestDTO {

    @NotBlank(message = "Flag 不能为空")
    private String flag;

}
