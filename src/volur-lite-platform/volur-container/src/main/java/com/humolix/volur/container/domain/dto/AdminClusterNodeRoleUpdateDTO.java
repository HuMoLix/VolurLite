package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterNodeRoleUpdateDTO {

    @NotBlank(message = "节点角色不能为空")
    private String role;

    @NotNull(message = "资源节点标识不能为空")
    private Boolean isResourceNode;

}
