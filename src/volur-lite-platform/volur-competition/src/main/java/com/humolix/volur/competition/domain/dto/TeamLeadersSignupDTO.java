package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.IdentificationCard;
import com.humolix.volur.framework.common.validation.annotation.Phone;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeadersSignupDTO {

    @NotBlank(message = "领队姓名不能为空")
    @Trimmed
    private String realName;

    private Gender gender;

    @NotBlank(message = "领队手机号不能为空")
    @Phone(message = "手机号格式不正确")
    @Trimmed
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Trimmed(toLowerCase = true)
    private String email;

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
