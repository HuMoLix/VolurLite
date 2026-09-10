package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "容器环境变量配置 (Environment Variables)")
public class ContainerEnvVarDTO implements Serializable {

    @Schema(description = "环境变量键名", example = "MYSQL_ROOT_PASSWORD")
    private String name;

    @Schema(description = "环境变量值", example = "root123456")
    private String value;

}
