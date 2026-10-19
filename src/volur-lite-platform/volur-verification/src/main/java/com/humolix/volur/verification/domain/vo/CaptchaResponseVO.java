package com.humolix.volur.verification.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人机验证响应视图 (精简规范版)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaptchaResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String captchaId;

    @Builder.Default
    private String captchaType = "IMAGE";

    private String imageBase64;

    private String sliderBase64;

    private Integer yPosition;

    private Boolean requireBehaviorVerify;

    public static CaptchaResponseVO of(String captchaId, String imageBase64) {
        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType("IMAGE")
                .imageBase64(imageBase64)
                .build();
    }

    public static CaptchaResponseVO of(String captchaId, String captchaType, String imageBase64) {
        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType(captchaType)
                .imageBase64(imageBase64)
                .build();
    }

}
