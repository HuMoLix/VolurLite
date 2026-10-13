package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionWriteupSubmitDTO {

    @NotBlank(message = "文件名不能为空")
    @Trimmed
    private String fileName;

    @NotBlank(message = "文件地址不能为空")
    @Trimmed
    private String fileUrl;

    private Long fileSize;

    @NotBlank(message = "验证码ID不能为空")
    @Trimmed
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    @Trimmed
    private String captchaCode;

}
