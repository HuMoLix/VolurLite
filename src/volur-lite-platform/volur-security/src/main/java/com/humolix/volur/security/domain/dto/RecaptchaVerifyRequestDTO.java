package com.humolix.volur.security.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "reCAPTCHA 安全挑战核验请求参数")
public class RecaptchaVerifyRequestDTO {

    @NotBlank(message = "人机挑战令牌 (recaptchaToken) 不能为空")
    @Schema(description = "挑战唯一令牌 ID", example = "rctok_1790527266429")
    private String recaptchaToken;

    @NotBlank(message = "加密验证数据 (payload) 不能为空")
    @Schema(description = "使用 RSA 公钥非对称加密的客户端行为与结果数据")
    private String payload;

}
