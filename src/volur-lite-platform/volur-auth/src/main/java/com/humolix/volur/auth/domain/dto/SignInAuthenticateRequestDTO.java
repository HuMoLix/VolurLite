package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInAuthenticateRequestDTO {

    @NotBlank(message = "分布式会话句柄 (dsh) 不能为空")
    private String dsh;

    @NotBlank(message = "状态流转票据 (TL) 不能为空")
    private String tl;

    @NotNull(message = "步骤序号 (cid) 不能为空")
    private Integer cid;

    private Boolean rememberMe;

}
