package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailOTPChallengeRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转票据不能为空")
    private String tl;

    private Integer cid;

    @NotBlank(message = "邮箱验证码不能为空")
    private String code;

    private Boolean rememberMe;

}
