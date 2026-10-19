package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterSettingsVO {

    private String containerType;

    private Integer portRangeStart;

    private Integer portRangeEnd;

    private String publicHost;

    private String dockerSwarmApiUrl;

    private String dockerSwarmNetwork;

    private String dockerSwarmCertPath;

    @Builder.Default
    private Boolean dockerSwarmOutboundEnabled = false;

    private String k3sMasterUrl;

    private String k3sNamespace;

    private String k3sKubeconfigPath;

    private String k3sToken;

    @Builder.Default
    private Boolean resourceIsolationEnabled = true;


    @Builder.Default
    private Integer totalNodes = 0;

    @Builder.Default
    private Integer masterNodes = 0;

    @Builder.Default
    private Integer workerNodes = 0;

    @Builder.Default
    private Integer readyNodes = 0;

}
