package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpEmailRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转锁票据不能为空")
    private String tl;

    @NotNull(message = "步骤序号不能为空")
    private Integer cid;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Trimmed
    private String email;

}
