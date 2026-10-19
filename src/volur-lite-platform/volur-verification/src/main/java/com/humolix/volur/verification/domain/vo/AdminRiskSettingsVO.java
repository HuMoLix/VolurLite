package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRiskSettingsVO {

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean ipFilterEnabled = true;

    @Builder.Default
    private Integer defaultBanDurationMinutes = 1440;

    @Builder.Default
    private String banNoticeMessage = "您的 IP 存在异常访问行为，已被系统安全机制拦截。如需解封请联系平台管理员。";

    @Builder.Default
    private Boolean bypassLoopback = true;

    // 人机验证与行为挑战设置
    @Builder.Default
    private Boolean captchaEnabled = true;

    @Builder.Default
    private Integer captchaTolerance = 8;

    @Builder.Default
    private Integer captchaTicketTtl = 300;

    @Builder.Default
    private Integer captchaMaxAttempts = 5;

    @Builder.Default
    private Boolean forceCaptchaOnReset = true;
}
