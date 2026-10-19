package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerTestConnectionVO {

    @Builder.Default
    private boolean connected = false;

    private String message;

    private String clusterVersion;

    @Builder.Default
    private Integer nodeCount = 0;

    private Long latencyMs;

}
