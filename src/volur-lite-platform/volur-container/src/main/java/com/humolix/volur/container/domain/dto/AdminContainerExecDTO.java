package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端容器内执行调试命令请求参数")
public class AdminContainerExecDTO {

    @NotBlank(message = "执行命令不能为空")
    @Schema(description = "待执行的 Shell 命令", example = "cat /flag", requiredMode = Schema.RequiredMode.REQUIRED)
    private String command;

    @Schema(description = "执行工作目录", example = "/app")
    private String workingDir;

    @Schema(description = "使用的解释器 (默认 /bin/sh)", example = "/bin/sh")
    private String shell;

}
