package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpPasswordRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转锁票据不能为空")
    private String tl;

    @NotNull(message = "步骤序号不能为空")
    private Integer cid;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

}
