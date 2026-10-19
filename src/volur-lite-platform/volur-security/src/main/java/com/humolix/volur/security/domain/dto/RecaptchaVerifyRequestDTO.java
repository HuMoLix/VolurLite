package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecaptchaVerifyRequestDTO {

    @NotBlank(message = "人机挑战令牌 (recaptchaToken) 不能为空")
    private String recaptchaToken;

    @NotBlank(message = "加密验证数据 (payload) 不能为空")
    private String payload;

}
