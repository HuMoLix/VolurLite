package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberInputDTO {

    @Trimmed
    private String name;

    @NotBlank(message = "成员手机号不能为空")
    @Trimmed
    private String phone;

    @Trimmed(toLowerCase = true)
    private String email;

    @Trimmed
    private String remark;

}
