package com.humolix.volur.container.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群 Docker 镜像深度详情视图 VO")
public class AdminClusterImageDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "镜像 ID")
    private Long id;

    @Schema(description = "镜像仓库名")
    private String repository;

    @Schema(description = "镜像标签 Tag")
    private String tag;

    @Schema(description = "完整镜像标识 (repository:tag)")
    private String fullImageName;

    @Schema(description = "镜像摘要 ID (sha256:...)")
    private String imageId;

    @Schema(description = "镜像体积(字节)")
    private Long sizeBytes;

    @Schema(description = "是否已在私有 Registry 中心注册托管")
    private Boolean inRegistry;

    @Schema(description = "操作系统 (如 linux)")
    private String os;

    @Schema(description = "体系架构 (如 amd64, arm64)")
    private String architecture;

    @Schema(description = "构建此镜像的 Docker 引擎版本")
    private String dockerVersion;

    @Schema(description = "作者/维护者信息")
    private String author;

    @Schema(description = "工作目录 (WORKDIR)")
    private String workingDir;

    @Schema(description = "运行用户 (USER)")
    private String user;

    @Schema(description = "入口程序 Entrypoint")
    private List<String> entrypoint;

    @Schema(description = "默认启动命令 Cmd")
    private List<String> cmd;

    @Schema(description = "环境变量列表 (ENV)")
    private List<String> env;

    @Schema(description = "暴露端口列表 (EXPOSE)")
    private List<String> exposedPorts;

    @Schema(description = "镜像元标签 Labels")
    private Map<String, String> labels;

    @Schema(description = "镜像历史分层 Layer 列表 (参考 Docker Hub 结构)")
    private List<DockerImageLayerVO> layers;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "首次拉取入库时间")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近更新时间")
    private Instant updatedAt;

}
