package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminIpRuleCreateDTO {

    @NotBlank(message = "IP 地址或网段不能为空")
    @Size(max = 64, message = "IP 地址或网段长度不能超过 64 字符")
    @Trimmed
    private String ip;

    @NotNull(message = "名单类型不能为空 (BLACKLIST / WHITELIST)")
    private IpRuleType ruleType;

    @Size(max = 255, message = "原因描述不能超过 255 字符")
    @Trimmed
    private String reason;

    /**
     * 封禁时长（分钟），若为 null 或小于等于 0 则表示永久生效
     */
    private Integer durationMinutes;

}
