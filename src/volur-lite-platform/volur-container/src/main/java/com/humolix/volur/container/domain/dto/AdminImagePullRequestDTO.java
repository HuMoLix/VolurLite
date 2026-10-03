package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImagePullRequestDTO {

    @NotBlank(message = "镜像名称与版本不能为空 (例如 ubuntu:22.04 或 volur/ctf-web:v1)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String image;

}
