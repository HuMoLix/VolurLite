package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "容器端口映射配置 (Ports)")
public class ContainerPortMappingDTO implements Serializable {

    @Schema(description = "宿主机映射端口 (留空则由系统在端口池 20000-30000 中自动分配)", example = "8080")
    private Integer hostPort;

    @Schema(description = "容器内部目标端口", example = "80")
    private Integer containerPort;

    @Schema(description = "协议类型 (TCP / UDP)", example = "TCP")
    private String protocol;

}
