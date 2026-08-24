package com.humolix.volur.verification.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyDTO {

    @NotBlank(message = "captchaId 不能为空")
    private String captchaId;

    @NotBlank(message = "验证输入不能为空")
    private String userInput;

    private String scene;

    private Long costTime;

}
