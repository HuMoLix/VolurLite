package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端测试容器集群连通性请求参数")
public class AdminContainerTestConnectionDTO {

    @Schema(description = "待测试的容器类型 (DOCKER_SWARM / K3S)，为空则读取系统设置中的默认类型")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String containerType;

    @Schema(description = "Docker Swarm API 地址 (可选，测试未保存配置时传递)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmApiUrl;

    @Schema(description = "Docker Swarm 证书路径 (可选)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmCertPath;

    @Schema(description = "K3s Master URL (可选)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sMasterUrl;

    @Schema(description = "K3s Kubeconfig 路径 (可选)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sKubeconfigPath;

    @Schema(description = "K3s 命名空间 (可选)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sNamespace;

}
