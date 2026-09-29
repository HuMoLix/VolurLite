package com.humolix.volur.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "登录安全挑战核验请求")
public class SignInChallengeRequestDTO {

    @NotBlank(message = "分布式会话句柄 (dsh) 不能为空")
    @Schema(description = "分布式会话状态句柄", example = "S-969965009:1790527266429740")
    private String dsh;

    @NotBlank(message = "状态流转票据 (TL) 不能为空")
    @Schema(description = "前序步骤签发的防重放流转锁票据", example = "ADG-GRTbPbNl0pSndZCtatZwpc273Y1KxgoCFYDnvVdrHspzQ9BKk03FfMrdB24l")
    private String tl;

    @NotNull(message = "步骤序号 (cid) 不能为空")
    @Schema(description = "当前挑战步骤序号", example = "1")
    private Integer cid;

    @NotBlank(message = "挑战类型不能为空")
    @Schema(description = "挑战类型: pwd(密码挑战), recaptcha(人机验证), 2fa(二次验证), device(设备确认)", example = "pwd")
    private String challengeType;

    @Schema(description = "用户密码 (当 challengeType=pwd 时必填)")
    private String password;

    @Schema(description = "人机验证码结果或 Token (当 challengeType=recaptcha 时使用)")
    private String captchaCode;

    @Schema(description = "两步验证 6 位动态验证码 (当 challengeType=2fa 时使用)")
    private String verificationCode;

    @Schema(description = "是否保持 30 天登录状态 / 信任设备", example = "false")
    private Boolean rememberMe;

}
