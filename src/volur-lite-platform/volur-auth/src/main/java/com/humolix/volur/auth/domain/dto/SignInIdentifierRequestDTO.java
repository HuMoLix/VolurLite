package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段化登录第一阶段：账号标识提交请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInIdentifierRequestDTO {

    @NotBlank(message = "登录账号或邮箱不能为空")
    @Trimmed
    private String identifier;

    @NotBlank
    private String dsh;

    private Integer cid = 0;

    private String deviceFingerprint;

}
