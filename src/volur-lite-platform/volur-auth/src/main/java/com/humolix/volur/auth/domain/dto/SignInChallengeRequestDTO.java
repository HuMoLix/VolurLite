package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段化登录第二/后续阶段：安全挑战核验请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInChallengeRequestDTO {

    @NotBlank(message = "分布式会话句柄 (dsh) 不能为空")
    private String dsh;

    @NotBlank(message = "状态流转票据 (TL) 不能为空")
    private String tl;

    @NotNull(message = "步骤序号 (cid) 不能为空")
    private Integer cid;

    @NotBlank(message = "挑战类型不能为空")
    private String challengeType;

    private String password;

    private String captchaCode;

    private String captchaId;

    private String captchaTicket;

    private String verificationCode;

    private Boolean rememberMe;

}
