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
@Schema(description = "reCAPTCHA 安全挑战获取响应视图")
public class RecaptchaChallengeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "挑战类型: CLICK, PUZZLE, SLIDER, IMAGE, SELECT_IMAGE", example = "PUZZLE")
    private String challengeType;

    @Schema(description = "挑战唯一令牌 ID", example = "rctok_1790527266429")
    private String recaptchaToken;

    @Schema(description = "RSA 公钥 (PEM 格式)")
    private String publicKey;

    @Schema(description = "背景图片 Base64 编码 (PUZZLE / SELECT_IMAGE)")
    private String backgroundImage;

    @Schema(description = "拼图滑块图片 Base64 编码 (PUZZLE)")
    private String puzzleImage;

    @Schema(description = "拼图 Y 轴坐标偏置 (PUZZLE)", example = "48")
    private Integer puzzleY;

    @Schema(description = "字符验证码图片 Base64 编码 (IMAGE)")
    private String captchaImage;

    @Schema(description = "提示文字", example = "拖动滑块完成拼图")
    private String promptText;

    @Schema(description = "扩展配置元数据")
    private Object extraData;

}
