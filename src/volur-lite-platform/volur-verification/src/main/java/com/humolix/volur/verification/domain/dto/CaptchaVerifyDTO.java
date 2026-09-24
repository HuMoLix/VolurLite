package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyDTO {

    @NotBlank(message = "captchaId 不能为空")
    @Trimmed
    private String captchaId;

    @NotBlank(message = "验证输入不能为空")
    @Trimmed
    private String userInput;

    @Trimmed
    private String scene;

    private Long costTime;

}
