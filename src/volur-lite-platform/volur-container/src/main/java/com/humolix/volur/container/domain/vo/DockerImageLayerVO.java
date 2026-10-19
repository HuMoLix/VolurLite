package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerImageLayerVO {

    private Integer layerIndex;

    private String layerId;

    private String instruction;

    private String createdBy;

    private Long sizeBytes;

    private String createdAt;

    private String comment;

}
