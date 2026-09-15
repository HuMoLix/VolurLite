package com.humolix.volur.challenge.domain.dto.awdp;

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
    private String name;

    private String description;

    private String category;

    private Integer difficulty;

    private String sourceCodeUrl;

    @NotBlank(message = "Docker镜像名不能为空")
    private String imageName;

    @NotNull(message = "容器服务端口不能为空")
    private Integer containerPort;

    private String patchVerifyScript;

    private String exploitVerifyScript;

}
