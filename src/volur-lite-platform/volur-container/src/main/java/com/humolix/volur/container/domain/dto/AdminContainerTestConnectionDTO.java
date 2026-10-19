package com.humolix.volur.container.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerTestConnectionDTO {

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String containerType;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmApiUrl;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmCertPath;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sMasterUrl;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sKubeconfigPath;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sNamespace;

}
