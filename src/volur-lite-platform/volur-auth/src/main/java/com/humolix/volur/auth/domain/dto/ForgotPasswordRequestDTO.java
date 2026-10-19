package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.Email;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确或邮件地址不合法")
    @Trimmed(toLowerCase = true)
    private String email;


    @NotBlank(message = "用户名不能为空")
    @Trimmed
    private String username;

    @Valid
    private CaptchaVerifyRequestDTO captcha;

}
