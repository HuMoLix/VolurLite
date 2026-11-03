package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCreateRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Trimmed
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Trimmed(toLowerCase = true)
    private String email;

    @Trimmed
    private String roleName;
}
