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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "集群 Docker 镜像详情视图 VO")
public class AdminClusterImageVO {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "首次拉取入库时间")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近一次更新或同步时间")
    private Instant updatedAt;

}
