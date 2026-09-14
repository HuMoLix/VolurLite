package com.humolix.volur.container.domain.vo;

import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端容器实例视图展示对象")
public class AdminContainerInstanceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "实例雪花 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "题目 ID")
    private Long challengeId;

    @Schema(description = "题目名称")
    private String challengeTitle;

    @Schema(description = "题目分类")
    private String challengeCategory;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "目标用户 ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String username;

    @Schema(description = "容器编排标识")
    private String containerId;

    @Schema(description = "动态靶机访问 URL")
    private String accessUrl;

    @Schema(description = "动态注入 Flag")
    private String flag;

    @Schema(description = "容器当前状态")
    private InstanceStartupStatus status;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "过期时间")
    private Instant expiresAt;

    @Schema(description = "剩余时长（秒）")
    private Integer remainingSeconds;

    @Schema(description = "容器类型 (DOCKER_SWARM / K3S)")
    private String containerType;

    @Schema(description = "Docker 镜像名称")
    private String imageName;

    @Schema(description = "容器自定义名称")
    private String containerName;

    @Schema(description = "端口映射配置")
    private String ports;

    @Schema(description = "数据卷挂载配置")
    private String volumes;

    @Schema(description = "环境变量配置")
    private String envVars;

    @Schema(description = "宿主运行节点 IP")
    private String nodeIp;

    @Schema(description = "宿主运行节点名称")
    private String nodeName;

    @Schema(description = "宿主运行节点角色 (MASTER / WORKER)")
    private String nodeRole;

    @Schema(description = "宿主节点是否脱机离线")
    private Boolean isNodeOffline;

}
