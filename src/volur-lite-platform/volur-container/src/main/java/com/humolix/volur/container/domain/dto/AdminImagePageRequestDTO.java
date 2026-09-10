package com.humolix.volur.container.domain.dto;

import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端 Docker 镜像分页查询请求 DTO")
public class AdminImagePageRequestDTO extends PageRequestDTO {

    @Schema(description = "搜索关键词 (镜像名/Tag/关联题目)")
    private String keyword;

    @Schema(description = "镜像类别筛选 (CHALLENGE / BASE / SYSTEM)")
    private String imageType;

    @Schema(description = "所在节点 IP 筛选")
    private String nodeIp;

}
