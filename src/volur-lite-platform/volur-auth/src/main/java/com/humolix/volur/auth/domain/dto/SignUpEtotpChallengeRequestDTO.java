package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpEtotpChallengeRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转锁票据不能为空")
    private String tl;

    @NotNull(message = "步骤序号不能为空")
    private Integer cid;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为 6 位数字")
    @Trimmed
    private String code;

}
