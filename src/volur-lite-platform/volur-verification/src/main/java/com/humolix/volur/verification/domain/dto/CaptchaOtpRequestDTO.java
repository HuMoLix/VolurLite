package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求下发带外 OTP 安全码入参 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaOtpRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Trimmed(toLowerCase = true)
    private String account;

    @Trimmed
    private String captchaTicket;

}
