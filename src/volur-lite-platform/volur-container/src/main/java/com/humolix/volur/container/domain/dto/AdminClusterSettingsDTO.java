package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterSettingsDTO {

    @NotBlank(message = "集群编排类型不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String containerType;

    @NotNull(message = "起始端口不能为空")
    @Min(value = 1024, message = "起始端口不能小于1024")
    @Max(value = 65535, message = "起始端口不能大于65535")
    private Integer portRangeStart;

    @NotNull(message = "结束端口不能为空")
    @Min(value = 1024, message = "结束端口不能小于1024")
    @Max(value = 65535, message = "结束端口不能大于65535")
    private Integer portRangeEnd;

    @NotBlank(message = "公网主机地址不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String publicHost;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmApiUrl;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmNetwork;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String dockerSwarmCertPath;

    private Boolean dockerSwarmOutboundEnabled;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sMasterUrl;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sNamespace;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sKubeconfigPath;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String k3sToken;

    private Boolean resourceIsolationEnabled;

}
