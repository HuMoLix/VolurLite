package com.humolix.volur.verification.domain.vo;

import com.humolix.volur.verification.domain.enums.IpRuleSource;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIpRuleVO {

    private Long id;

    private String ip;

    private IpRuleType ruleType;

    private IpRuleSource sourceType;

    private String reason;

    private IpRuleStatus status;

    private Instant expiresAt;

    private Long operatorId;

    private String operatorName;

    private String operatorIp;

    private Instant createdAt;

    private Instant updatedAt;

    @Builder.Default
    private Boolean isEffective = true;

}
