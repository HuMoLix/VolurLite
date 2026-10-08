package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确或邮件地址不合法")
    @Trimmed(toLowerCase = true)
    private String email;


    @NotBlank(message = "验证码不能为空")
    @Trimmed
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6到32个字符之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32}$",
            message = "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间"
    )
    private String password;

}
