package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "邮箱验证码不能为空")
    @Trimmed
    private String code;

    @NotBlank(message = "确认用户名不能为空")
    @Trimmed
    private String username;

    @Valid
    private CaptchaVerifyRequestDTO captcha;

}
