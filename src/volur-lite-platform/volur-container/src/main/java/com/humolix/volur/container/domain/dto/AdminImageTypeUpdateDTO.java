package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "修改镜像类别请求 DTO")
public class AdminImageTypeUpdateDTO {

    @NotBlank(message = "镜像类别不能为空")
    @Schema(description = "镜像类别 (CHALLENGE / BASE / SYSTEM)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageType;

}
