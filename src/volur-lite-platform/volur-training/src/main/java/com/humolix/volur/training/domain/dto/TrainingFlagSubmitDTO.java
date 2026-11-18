package com.humolix.volur.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingFlagSubmitDTO {

    @NotBlank(message = "flag 不能为空")
    private String flag;

}
