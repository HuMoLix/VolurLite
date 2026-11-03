package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImageTransferDTO {

    @NotNull(message = "镜像ID不能为空")
    private Long imageId;

    @NotBlank(message = "源节点IP不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String sourceNodeIp;

    @NotBlank(message = "目标转移节点IP不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String targetNodeIp;

}
