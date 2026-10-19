package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人机验证自适应挑战决策视图 (CaptchaSDK 挑战探测响应)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaChallengeDecisionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前评估的风控等级 (LOW, MEDIUM, HIGH, BLOCKED)
     */
    private String riskLevel;

    /**
     * 前端交互推荐呈现模式: NONE (免验), IMAGE (字符图形码), SLIDER (滑块拼图), EMAIL_OTP (邮件安全码)
     */
    @Builder.Default
    private String mode = "IMAGE";

    /**
     * 是否需要进行行为式拼图验证
     */
    @Builder.Default
    private Boolean requireBehaviorVerify = false;

    /**
     * 是否需要进行带外邮箱 OTP 动态安全码验证
     */
    @Builder.Default
    private Boolean requireOtp = false;

    /**
     * 若返回字符图形验证码，其唯一 ID
     */
    private String captchaId;

    /**
     * 若返回字符图形验证码，其 Base64 图片数据
     */
    private String imageBase64;

    /**
     * 验证码子类型 (LINE, SHEAR 等)
     */
    private String captchaType;

}
