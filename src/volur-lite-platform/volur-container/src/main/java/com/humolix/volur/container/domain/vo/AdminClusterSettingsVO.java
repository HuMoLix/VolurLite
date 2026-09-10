package com.humolix.volur.container.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群配置视图 VO")
public class AdminClusterSettingsVO {

    @Schema(description = "容器集群调度类型(DOCKER_SWARM/K3S)")
    private String containerType;

    @Schema(description = "靶机动态端口映射起始端口")
    private Integer portRangeStart;

    @Schema(description = "靶机动态端口映射结束端口")
    private Integer portRangeEnd;

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

    @Builder.Default
    @Schema(description = "是否开启多节点资源隔离(题目容器仅调度至Worker资源节点)")
    private Boolean resourceIsolationEnabled = true;

    @Builder.Default
    @Schema(description = "扩展集群新增节点时是否自动将主节点题目镜像迁移同步至资源节点")
    private Boolean autoMigrateImagesEnabled = true;

    @Builder.Default
    @Schema(description = "总节点数")
    private Integer totalNodes = 0;

    @Builder.Default
    @Schema(description = "主节点数")
    private Integer masterNodes = 0;

    @Builder.Default
    @Schema(description = "资源工作节点数")
    private Integer workerNodes = 0;

    @Builder.Default
    @Schema(description = "就绪节点数")
    private Integer readyNodes = 0;

}
