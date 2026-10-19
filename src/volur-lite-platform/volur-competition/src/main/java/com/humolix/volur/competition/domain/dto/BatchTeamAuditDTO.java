package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.TeamStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchTeamAuditDTO {

    @NotEmpty(message = "队伍ID列表不能为空")
    private List<Long> teamIds;

    @NotNull(message = "审核状态不能为空")
    private TeamStatus status;

    @Size(max = 500, message = "审核驳回原因不能超过 500 个字符")
    private String auditReason;

}
