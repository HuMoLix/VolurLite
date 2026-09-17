package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponseVO {

    private String captchaId;

    @Builder.Default
    private String captchaType = "LINE";

    private String imageBase64;

    private String sliderBase64;

    private Integer yPosition;

    @Builder.Default
    private Boolean requireBehaviorVerify = false;

    public static CaptchaResponseVO of(String captchaId, String imageBase64) {
        return CaptchaResponseVO.builder()
                .captchaId(captchaId)
                .captchaType("LINE")
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
