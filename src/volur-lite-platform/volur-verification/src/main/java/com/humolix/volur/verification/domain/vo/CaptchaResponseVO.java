package com.humolix.volur.verification.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "人机验证响应视图")
public class CaptchaResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码唯一会话挑战 ID")
    private String captchaId;

    @Schema(description = "当前交互推荐呈现的验证码类型: IMAGE, SLIDER, EMAIL, SMS, NONE")
    @Builder.Default
    private String captchaType = "IMAGE";

    @Schema(description = "字符图形验证码 Base64 编码图片（仅当 captchaType=IMAGE 时下发）")
    private String imageBase64;

    @Schema(description = "滑块拼图切片 Base64 编码图片（仅行为式拼图挑战专用）")
    private String sliderBase64;

    @Schema(description = "滑块拼图缺口纵坐标 Y（仅行为式拼图挑战专用）")
    private Integer yPosition;

    @Schema(description = "内部风控行为式验证标识（兼容性保留）")
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
