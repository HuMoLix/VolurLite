package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDTO implements Serializable {

    @NotBlank(message = "邮箱验证码不能为空")
    private String code;

    @NotBlank(message = "确认用户名不能为空")
    private String username;

    private String captchaId;

    private String captchaCode;

    private String captchaTicket;

}
