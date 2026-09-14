package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "下载拉取 Docker 镜像请求 DTO")
public class AdminImagePullRequestDTO {

    @NotBlank(message = "镜像名称与版本不能为空 (例如 ubuntu:22.04 或 volur/ctf-web:v1)")
    @Schema(description = "Docker 镜像名称 (含Tag)")
    private String image;

}
