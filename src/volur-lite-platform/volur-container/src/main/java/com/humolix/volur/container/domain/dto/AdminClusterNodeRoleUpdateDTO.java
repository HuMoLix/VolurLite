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
@Schema(description = "修改集群节点角色与资源调度设置 DTO")
public class AdminClusterNodeRoleUpdateDTO {

    @NotBlank(message = "节点角色不能为空")
    @Schema(description = "节点角色 (MASTER / WORKER)")
    private String role;

    @NotNull(message = "资源节点标识不能为空")
    @Schema(description = "是否作为赛题资源节点")
    private Boolean isResourceNode;

}
