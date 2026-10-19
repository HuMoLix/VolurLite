package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImagePullRequestDTO {

    public AdminImagePullRequestDTO(String image) {
        this.image = image;
    }

    @NotBlank(message = "镜像名称与版本不能为空 (例如 ubuntu:22.04 或 ghcr.io/org/image:tag)")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String image;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String authServer;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String username;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String password;

}
