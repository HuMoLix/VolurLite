package com.humolix.volur.verification.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "自适应人机验证码请求 DTO")
public class CaptchaGenerateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "经前端魔改 RC4 + RSA 加密后包含源路由与防重放时间戳的场景凭据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "场景安全凭据不能为空")
    private String captchaSceneId;

    @Schema(description = "当前操作的用户名或账号（用于账号级风控评估）")
    private String account;

}
