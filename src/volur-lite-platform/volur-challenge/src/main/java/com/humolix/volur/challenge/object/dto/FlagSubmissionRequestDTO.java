package com.humolix.volur.challenge.object.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlagSubmissionRequestDTO {

    @NotBlank(message = "Flag不能为空")
    private String flag;

}
