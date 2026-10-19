package com.humolix.volur.container.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerManualStartDTO implements Serializable {

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String imageName;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String containerName;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String nodeIp;

    @Deprecated
    private Long challengeId;

    private Long userId;

    private Integer durationMinutes;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String customFlag;

    private Boolean outboundEnabled;

    private List<ContainerPortMappingDTO> ports;

    private List<ContainerVolumeMappingDTO> volumes;

    private List<ContainerEnvVarDTO> environments;

    private Integer cpuLimit;

    private Integer memoryLimit;

}
