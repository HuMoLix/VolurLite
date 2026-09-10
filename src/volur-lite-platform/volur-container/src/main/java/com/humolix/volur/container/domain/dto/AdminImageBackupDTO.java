package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "镜像备份请求 DTO")
public class AdminImageBackupDTO {

    @NotNull(message = "镜像ID不能为空")
    @Schema(description = "待备份镜像ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long imageId;

    @NotBlank(message = "备份模式不能为空")
    @Schema(description = "备份模式: NODE (节点容灾副本) 或 ARCHIVE (本地离线包归档)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String backupType;

    @Schema(description = "备份目标节点IP (当 backupType 为 NODE 时使用)")
    private String targetNodeIp;

}
