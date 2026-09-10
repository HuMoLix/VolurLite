package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端延长容器生命周期请求参数")
public class AdminContainerRenewRequestDTO {

    @Schema(description = "延长时间（分钟，范围 5 ~ 120）", example = "30")
    @Min(value = 5, message = "续期时长至少 5 分钟")
    @Max(value = 120, message = "单次续期时长最多 120 分钟")
    private int durationMinutes = 30;

}
