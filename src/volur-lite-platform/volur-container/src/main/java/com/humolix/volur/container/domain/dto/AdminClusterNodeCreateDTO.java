package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "扩展添加集群节点请求 DTO")
public class AdminClusterNodeCreateDTO {

    @NotBlank(message = "节点名称不能为空")
    @Size(min = 2, max = 64, message = "节点名称长度在2-64个字符之间")
    @Schema(description = "节点标识名称")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String nodeName;

    @NotBlank(message = "节点IP地址不能为空")
    @Size(min = 7, max = 64, message = "IP地址长度格式不正确")
    @Schema(description = "节点 IP 地址")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String ipAddress;

    @NotNull(message = "SSH端口不能为空")
    @Min(value = 1, message = "SSH端口范围1-65535")
    @Max(value = 65535, message = "SSH端口范围1-65535")
    @Schema(description = "节点 SSH 端口 (必须)")
    private Integer sshPort = 22;

    @NotBlank(message = "集群类别不能为空")
    @Schema(description = "集群类别 (DOCKER_SWARM 或 K3S)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String clusterType;

    @NotBlank(message = "节点角色不能为空")
    @Schema(description = "节点角色 (MASTER 或 WORKER)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String role = "WORKER";

    @Schema(description = "是否指定为题目资源节点 (Worker节点自动等价为资源节点)")
    private Boolean isResourceNode = true;

    @Schema(description = "是否启用 SSH 自动认证配置与纳管")
    private Boolean enableSshAuth = true;

    @NotBlank(message = "SSH登录用户名不能为空")
    @Size(min = 1, max = 64, message = "SSH用户名长度在1-64个字符之间")
    @Schema(description = "SSH 登录用户名 (必须, 默认 root)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String sshUser = "root";

    @Schema(description = "SSH 连接凭据密码 (与私钥二选一)")
    private String sshPassword;

    @Schema(description = "SSH 私钥凭据 (与密码二选一)")
    private String sshKey;

}
