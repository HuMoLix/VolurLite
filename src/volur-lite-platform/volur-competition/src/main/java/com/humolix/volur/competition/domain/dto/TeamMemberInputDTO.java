package com.humolix.volur.competition.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberInputDTO {

    private String name;

    @NotBlank(message = "成员手机号不能为空")
    private String phone;

    private String email;

    private String remark;

}
