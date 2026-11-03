package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerExecDTO {

    @NotBlank(message = "执行命令不能为空")
    private String command;

    private String workingDir;

    private String shell;

}
