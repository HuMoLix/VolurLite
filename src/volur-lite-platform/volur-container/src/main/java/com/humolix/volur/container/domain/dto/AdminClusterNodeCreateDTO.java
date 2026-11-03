package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterNodeCreateDTO {

    @NotBlank(message = "节点名称不能为空")
    @Size(min = 2, max = 64, message = "节点名称长度在2-64个字符之间")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String nodeName;

    @NotBlank(message = "节点IP地址不能为空")
    @Size(min = 7, max = 64, message = "IP地址长度格式不正确")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String ipAddress;

    @NotNull(message = "SSH端口不能为空")
    @Min(value = 1, message = "SSH端口范围1-65535")
    @Max(value = 65535, message = "SSH端口范围1-65535")
    private Integer sshPort = 22;

    @NotBlank(message = "集群类别不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String clusterType;

    @NotBlank(message = "节点角色不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String role = "WORKER";

    private Boolean isResourceNode = true;

    private Boolean enableSshAuth = true;

    @NotBlank(message = "SSH登录用户名不能为空")
    @Size(min = 1, max = 64, message = "SSH用户名长度在1-64个字符之间")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String sshUser = "root";

    private String sshPassword;

    private String sshKey;

}
