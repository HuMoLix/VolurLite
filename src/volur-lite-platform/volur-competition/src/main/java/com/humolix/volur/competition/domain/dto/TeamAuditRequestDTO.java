package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.TeamStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAuditRequestDTO {

    @NotNull(message = "审核状态不能为空")
    private TeamStatus status;

    @com.humolix.volur.framework.common.annotation.Trimmed
    private String auditReason;

}
