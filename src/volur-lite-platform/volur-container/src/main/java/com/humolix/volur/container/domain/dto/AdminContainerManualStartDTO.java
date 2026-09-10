package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端手动启动容器实例请求参数")
public class AdminContainerManualStartDTO implements Serializable {

    @Schema(description = "Docker 镜像名称与 Tag (例如 'nginx:alpine' 或 'vulhub/activemq:5.11.1')")
    private String imageName;

    @Schema(description = "自定义容器/服务名称 (Container name)")
    private String containerName;

    @Schema(description = "指定部署运行的目标 Worker 节点 IP (若不指定或单节点集群则运行在可用资源节点)")
    private String nodeIp;

    @Deprecated
    @Schema(description = "关联赛题 ID (已废弃，管理员手动启动容器独立运行于题目体系)")
    private Long challengeId;

    @Schema(description = "绑定的目标用户 ID (可选，若不指定则绑定至当前管理员用户)")
    private Long userId;

    @Schema(description = "容器初始存活时长 (分钟，默认 30 分钟，输入 0 表示长期运行)")
    private Integer durationMinutes;

    @Schema(description = "自定义动态 Flag (可选，若不填写则按规则自动注入)")
    private String customFlag;

    @Schema(description = "端口映射列表 (Ports)")
    private List<ContainerPortMappingDTO> ports;

    @Schema(description = "数据卷挂载列表 (Volumes)")
    private List<ContainerVolumeMappingDTO> volumes;

    @Schema(description = "环境变量列表 (Environment variables)")
    private List<ContainerEnvVarDTO> environments;

    @Schema(description = "CPU 限制 (核数，默认 1)")
    private Integer cpuLimit;

    @Schema(description = "内存限制 (MB，默认 256)")
    private Integer memoryLimit;

}
