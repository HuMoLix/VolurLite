package com.humolix.volur.security.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "reCAPTCHA 安全挑战核验响应视图")
public class RecaptchaVerifyResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否核验通过", example = "true")
    private boolean passed;

    @Schema(description = "核发的人机通过通行凭证 (recaptchaTicket)", example = "rctk_7b99c7356a634421b51e069c9cf")
    private String recaptchaTicket;

}
