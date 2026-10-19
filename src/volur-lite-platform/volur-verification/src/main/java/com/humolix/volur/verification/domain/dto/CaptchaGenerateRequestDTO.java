package com.humolix.volur.verification.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 获取人机验证码/自适应挑战请求入参 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaGenerateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "场景安全凭据不能为空")
    private String captchaSceneId;

    private String account;

}
