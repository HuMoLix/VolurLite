package com.humolix.volur.competition.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInviteRequestDTO {

    @NotNull(message = "被邀请用户ID不能为空")
    private Long inviteeUserId;

    private String remark;

}
