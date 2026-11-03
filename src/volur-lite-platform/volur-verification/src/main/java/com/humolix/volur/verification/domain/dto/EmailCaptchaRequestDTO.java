package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCaptchaRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Trimmed(toLowerCase = true)
    private String email;

    @Trimmed
    private String username;

    @Valid
    private CaptchaVerifyRequestDTO captcha;

}
