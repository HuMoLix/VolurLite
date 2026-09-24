package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.verification.domain.enums.IpRuleSource;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminIpRuleQueryDTO {

    private Integer page = 1;

    private Integer size = 20;

    @Trimmed
    private String ip;

    private IpRuleType ruleType;

    private IpRuleSource sourceType;

    private IpRuleStatus status;

    private String sortBy = "createdAt";

    private String sortOrder = "desc";

}
