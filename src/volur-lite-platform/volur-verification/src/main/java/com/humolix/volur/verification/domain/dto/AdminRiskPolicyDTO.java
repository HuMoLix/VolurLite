package com.humolix.volur.verification.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRiskPolicyDTO {

    // ========== 1. 邮件策略 (Email Policy) ==========
    private List<String> emailWhitelistSuffixes = new ArrayList<>();

    // ========== 2. 拦截策略 (Interception Policy) ==========
    @NotNull(message = "限流时间窗口不能为空")
    @Min(value = 1, message = "限流窗口至少为 1 秒")
    @Max(value = 3600, message = "限流窗口不能超过 3600 秒")
    private Integer rateLimitWindow;

    @NotNull(message = "窗口内最大请求次数不能为空")
    @Min(value = 1, message = "最大请求次数至少为 1")
    @Max(value = 100000, message = "最大请求次数不能超过 100000")
    private Integer rateLimitMaxRequests;

    @NotNull(message = "1分钟突发速率阈值不能为空")
    @Min(value = 1, message = "速率阈值至少为 1")
    @Max(value = 1000, message = "速率阈值不能超过 1000")
    private Integer velocityMediumThreshold;

    @NotNull(message = "单账号全网失败升中风险阈值不能为空")
    @Min(value = 1, message = "阈值至少为 1")
    @Max(value = 100, message = "阈值不能超过 100")
    private Integer accountFailuresMediumThreshold;

    @NotNull(message = "单账号全网失败升高风险阈值不能为空")
    @Min(value = 2, message = "阈值至少为 2")
    @Max(value = 100, message = "阈值不能超过 100")
    private Integer accountFailuresHighThreshold;

    // ========== 3. 封禁策略 (Ban Policy) ==========
    @NotNull(message = "连续失败升中风险阈值不能为空")
    @Min(value = 1, message = "阈值至少为 1")
    @Max(value = 100, message = "阈值不能超过 100")
    private Integer failuresMediumThreshold;

    @NotNull(message = "连续失败升高风险阈值不能为空")
    @Min(value = 2, message = "阈值至少为 2")
    @Max(value = 100, message = "阈值不能超过 100")
    private Integer failuresHighThreshold;

    @NotNull(message = "自动封禁阈值不能为空")
    @Min(value = 0, message = "阈值至少为 0 (0 表示不开启自动封禁)")
    @Max(value = 200, message = "阈值不能超过 200")
    private Integer failuresBanThreshold;

    @NotNull(message = "降险扣减步长不能为空")
    @Min(value = 1, message = "扣减步长至少为 1")
    @Max(value = 50, message = "扣减步长不能超过 50")
    private Integer recoveryStepCount;

}
