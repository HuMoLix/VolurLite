package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.IdentificationCard;
import com.humolix.volur.framework.common.validation.annotation.Phone;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.validation.constraints.Email;
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
    @Trimmed
    private String realName;

    @NotNull(message = "性别不能为空")
    private Gender gender;

    @NotNull(message = "角色不能为空")
    private MemberRole role;

    @NotBlank(message = "手机号不能为空")
    @Phone(message = "手机号格式不正确")
    @Trimmed
    private String phone;

    @NotBlank(message = "电子邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Trimmed(toLowerCase = true)
    private String email;

    @NotBlank(message = "身份证号不能为空")
    @IdentificationCard(message = "身份证号格式不正确")
    @Trimmed
    private String idCard;

    @Trimmed
    private String employeeNumber;

    @Trimmed
    private String department;

    @Trimmed
    private String position;

}
