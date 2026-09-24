package com.humolix.volur.challenge.domain.dto.awdp;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdPlusChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    @Trimmed
    private String name;

    private String description;

    @Trimmed
    private String category;

    private Integer difficulty;

    @Trimmed
    private String sourceCodeUrl;

    @NotBlank(message = "Docker镜像名不能为空")
    @Trimmed
    private String imageName;

    @NotNull(message = "容器服务端口不能为空")
    private Integer containerPort;

    private String patchVerifyScript;

    private String exploitVerifyScript;

}
