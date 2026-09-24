package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "镜像节点转移请求 DTO")
public class AdminImageTransferDTO {

    @NotNull(message = "镜像ID不能为空")
    @Schema(description = "目标镜像ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long imageId;

    @NotBlank(message = "源节点IP不能为空")
    @Schema(description = "镜像当前所在的源节点IP", requiredMode = Schema.RequiredMode.REQUIRED)
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String sourceNodeIp;

    @NotBlank(message = "目标转移节点IP不能为空")
    @Schema(description = "转移目的节点IP", requiredMode = Schema.RequiredMode.REQUIRED)
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String targetNodeIp;

}
