package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理端修改参赛队伍成员信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeamMemberDTO implements Serializable {

    private Long id;

    @Size(max = 64, message = "成员姓名长度不能超过 64 个字符")
    private String realName;

    private Gender gender;

    private MemberRole role;

    @Size(max = 32, message = "手机号长度不能超过 32 个字符")
    private String phone;

    @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
    private String email;

    @Size(max = 32, message = "身份证号长度不能超过 32 个字符")
    private String idCard;

    @Size(max = 64, message = "学号/工号长度不能超过 64 个字符")
    private String employeeNumber;

    @Size(max = 128, message = "学院/部门长度不能超过 128 个字符")
    private String department;

    @Size(max = 64, message = "班级/职位长度不能超过 64 个字符")
    private String position;

}
