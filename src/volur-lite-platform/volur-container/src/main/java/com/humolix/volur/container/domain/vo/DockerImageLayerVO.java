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
@Schema(description = "Docker 镜像单层 Layer 详情视图 VO")
public class DockerImageLayerVO {

    @Schema(description = "Layer 顺序索引 (从基础层向上递增)")
    private Integer layerIndex;

    @Schema(description = "Layer 唯一摘要标识 (sha256:...)")
    private String layerId;

    @Schema(description = "指令类型 (如 RUN, COPY, CMD, ENV, EXPOSE, WORKDIR 等)")
    private String instruction;

    @Schema(description = "原始构建命令 (CreatedBy)")
    private String createdBy;

    @Schema(description = "该层体积(字节)")
    private Long sizeBytes;

    @Schema(description = "格式化体积 (如 12.5 MB 或 0 B)")
    private String sizeFormatted;

    @Schema(description = "创建时间或相对时间")
    private String createdAt;

    @Schema(description = "该层注释说明")
    private String comment;

}
