package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImageBackupDTO {

    @NotNull(message = "镜像ID不能为空")
    private Long imageId;

    @NotBlank(message = "备份模式不能为空")
    private String backupType;

    private String targetNodeIp;

}
