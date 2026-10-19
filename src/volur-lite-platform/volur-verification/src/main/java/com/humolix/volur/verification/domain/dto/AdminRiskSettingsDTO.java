package com.humolix.volur.verification.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRiskSettingsDTO {

    @NotNull(message = "全局风控开关不能为空")
    private Boolean enabled;

    @NotNull(message = "IP 拦截总开关不能为空")
    private Boolean ipFilterEnabled;

    @NotNull(message = "默认封禁时长不能为空")
    @Min(value = 1, message = "封禁时长至少为 1 分钟")
    @Max(value = 525600, message = "封禁时长不能超过 1 年")
    private Integer defaultBanDurationMinutes;

    @NotBlank(message = "拦截提示文案不能为空")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String banNoticeMessage;

    @NotNull(message = "本地回环放行开关不能为空")
    private Boolean bypassLoopback;

    @NotNull(message = "人机验证开关不能为空")
    private Boolean captchaEnabled;

    @NotNull(message = "滑块容差阈值不能为空")
    @Min(value = 2, message = "滑块容差至少为 2px")
    @Max(value = 25, message = "滑块容差最大为 25px")
    private Integer captchaTolerance;

    @NotNull(message = "凭证有效期不能为空")
    @Min(value = 60, message = "凭证有效期至少为 60 秒")
    @Max(value = 1800, message = "凭证有效期最大为 1800 秒")
    private Integer captchaTicketTtl;

    @NotNull(message = "最大尝试次数不能为空")
    @Min(value = 3, message = "最大尝试次数至少为 3 次")
    @Max(value = 20, message = "最大尝试次数最大为 20 次")
    private Integer captchaMaxAttempts;

    @NotNull(message = "重置密码强制人机验证开关不能为空")
    private Boolean forceCaptchaOnReset;
}
