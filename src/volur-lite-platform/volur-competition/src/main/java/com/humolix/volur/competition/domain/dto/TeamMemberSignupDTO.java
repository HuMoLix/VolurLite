package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberSignupDTO {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "性别不能为空")
    private Gender gender;

    @NotNull(message = "角色不能为空")
    private MemberRole role;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    @NotBlank(message = "电子邮箱不能为空")
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String organization;

    private String department;

    private String position;

}
