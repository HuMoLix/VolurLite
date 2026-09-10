package com.humolix.volur.container.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群节点详情视图 VO")
public class AdminClusterNodeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "节点 ID")
    private Long id;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点 IP 地址")
    private String ipAddress;

    @Schema(description = "节点 SSH 端口")
    private Integer sshPort;

    @Schema(description = "SSH 登录用户名")
    private String sshUser;

    @Schema(description = "是否已配置 SSH 认证凭据")
    private Boolean hasSshAuth;

    @Schema(description = "SSH 认证方式 (password 或 key)")
    private String authType;

    @Schema(description = "Docker 远程端口 (2376 / 2375)")
    private Integer dockerPort;

    @Schema(description = "是否已启用 TLS 双向证书加密")
    private Boolean tlsEnabled;

    @Schema(description = "TLS 证书在平台中的存储路径")
    private String tlsCertPath;

    @Schema(description = "集群类别 (DOCKER_SWARM 或 K3S)")
    private String clusterType;

    @Schema(description = "节点角色 (MASTER 或 WORKER)")
    private String role;

    @Schema(description = "是否作为赛题资源节点")
    private Boolean isResourceNode;

    @Schema(description = "节点状态 (READY / JOINING / OFFLINE / ERROR)")
    private String status;

    @Schema(description = "操作系统信息")
    private String osInfo;

    @Schema(description = "Docker / containerd 引擎版本")
    private String dockerVersion;

    @Schema(description = "CPU 核心数")
    private Integer cpuCores;

    @Schema(description = "内存总量(字节)")
    private Long memoryBytes;

    @Schema(description = "格式化内存")
    private String memoryFormatted;

    @Schema(description = "当前运行容器数")
    private Integer containerCount;

    @Schema(description = "本地镜像缓存数")
    private Integer imageCount;

    @Schema(description = "是否已真正加入底层编排集群 (Docker Swarm / K3s)")
    private Boolean inCluster;

    @Schema(description = "底层编排集群分配的唯一节点 ID (如 Swarm Node ID)")
    private String clusterNodeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "加入集群时间")
    private Instant joinTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近一次心跳汇报时间")
    private Instant lastHeartbeat;

}
