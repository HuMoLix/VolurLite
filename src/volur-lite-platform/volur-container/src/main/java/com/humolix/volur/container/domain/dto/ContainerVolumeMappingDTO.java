package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "容器数据卷挂载配置 (Volumes)")
public class ContainerVolumeMappingDTO implements Serializable {

    @Schema(description = "宿主机绝对路径 (Host Path)", example = "/opt/volur/data")
    private String hostPath;

    @Schema(description = "容器内部挂载路径 (Container Path)", example = "/var/www/html")
    private String containerPath;

    @Schema(description = "挂载模式 (rw: 读写, ro: 只读)", example = "rw")
    private String mode;

}
