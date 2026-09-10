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
@Schema(description = "修改集群节点信息请求 DTO")
public class AdminClusterNodeUpdateDTO {

    @NotBlank(message = "节点名称不能为空")
    @Size(min = 2, max = 64, message = "节点名称长度在2-64个字符之间")
    @Schema(description = "节点标识名称")
    private String nodeName;

    @NotBlank(message = "节点IP地址不能为空")
    @Size(min = 7, max = 64, message = "IP地址长度格式不正确")
    @Schema(description = "节点 IP 地址")
    private String ipAddress;

    @NotNull(message = "SSH端口不能为空")
    @Min(value = 1, message = "SSH端口范围1-65535")
    @Max(value = 65535, message = "SSH端口范围1-65535")
    @Schema(description = "节点 SSH 端口")
    private Integer sshPort = 22;

    @NotBlank(message = "SSH登录用户名不能为空")
    @Size(min = 1, max = 64, message = "SSH用户名长度在1-64个字符之间")
    @Schema(description = "SSH 登录用户名")
    private String sshUser = "root";

    @Schema(description = "SSH 连接凭据密码 (留空则不修改)")
    private String sshPassword;

    @Schema(description = "SSH 私钥凭据 (留空则不修改)")
    private String sshKey;

    @NotBlank(message = "节点角色不能为空")
    @Schema(description = "节点角色 (MASTER 或 WORKER)")
    private String role;

    @Schema(description = "是否作为题目资源节点")
    private Boolean isResourceNode;

}
