package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群配置更新请求 DTO")
public class AdminClusterSettingsDTO {

    @NotBlank(message = "集群编排类型不能为空")
    @Schema(description = "容器集群调度类型(DOCKER_SWARM/K3S)")
    private String containerType;

    @NotNull(message = "起始端口不能为空")
    @Min(value = 1024, message = "起始端口不能小于1024")
    @Max(value = 65535, message = "起始端口不能大于65535")
    @Schema(description = "靶机动态端口映射起始端口")
    private Integer portRangeStart;

    @NotNull(message = "结束端口不能为空")
    @Min(value = 1024, message = "结束端口不能小于1024")
    @Max(value = 65535, message = "结束端口不能大于65535")
    @Schema(description = "靶机动态端口映射结束端口")
    private Integer portRangeEnd;

    @NotBlank(message = "公网主机地址不能为空")
    @Schema(description = "靶机外部可访问域名或IP")
    private String publicHost;

    @Schema(description = "Docker Swarm API 管理地址")
    private String dockerSwarmApiUrl;

    @Schema(description = "Docker Swarm 覆盖网络名称")
    private String dockerSwarmNetwork;

    @Schema(description = "Docker Swarm TLS 客户端证书目录")
    private String dockerSwarmCertPath;

    @Schema(description = "K3s 集群 API Server 地址")
    private String k3sMasterUrl;

    @Schema(description = "K3s 靶机命名空间 Namespace")
    private String k3sNamespace;

    @Schema(description = "K3s kubeconfig 配置文件绝对路径")
    private String k3sKubeconfigPath;

    @Schema(description = "是否开启多节点资源隔离(题目容器仅调度至Worker资源节点)")
    private Boolean resourceIsolationEnabled;

    @Schema(description = "扩展集群新增节点时是否自动将主节点题目镜像迁移同步至资源节点")
    private Boolean autoMigrateImagesEnabled;

}
