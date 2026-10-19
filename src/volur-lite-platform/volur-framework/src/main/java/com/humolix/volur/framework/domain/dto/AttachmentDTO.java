package com.humolix.volur.framework.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDTO {

    @NotBlank(message = "文件名称不能为空")
    private String fileName;

    @NotBlank(message = "文件URL不能为空")
    private String fileUrl;

    @NotBlank(message = "文件校验值不能为空")
    private String fileCheckSum;
}
