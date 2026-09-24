package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRiskPolicyVO {

    // ========== 1. 邮件策略 (Email Policy) ==========
    @Builder.Default
    private List<String> emailWhitelistSuffixes = new ArrayList<>();

    // ========== 2. 拦截策略 (Interception Policy) ==========
    @Builder.Default
    private Integer rateLimitWindow = 60;

    @Builder.Default
    private Integer rateLimitMaxRequests = 120;

    @Builder.Default
    private Integer velocityMediumThreshold = 12;

    @Builder.Default
    private Integer accountFailuresMediumThreshold = 4;

    @Builder.Default
    private Integer accountFailuresHighThreshold = 8;

    // ========== 3. 封禁策略 (Ban Policy) ==========
    @Builder.Default
    private Integer failuresMediumThreshold = 5;

    @Builder.Default
    private Integer failuresHighThreshold = 10;

    @Builder.Default
    private Integer failuresBanThreshold = 20;

    @Builder.Default
    private Integer recoveryStepCount = 6;

}
