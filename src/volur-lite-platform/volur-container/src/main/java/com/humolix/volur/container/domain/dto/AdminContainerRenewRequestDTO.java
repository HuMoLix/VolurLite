package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerRenewRequestDTO {

    @Min(value = 5, message = "续期时长至少 5 分钟")
    @Max(value = 120, message = "单次续期时长最多 120 分钟")
    private int durationMinutes = 30;

}
