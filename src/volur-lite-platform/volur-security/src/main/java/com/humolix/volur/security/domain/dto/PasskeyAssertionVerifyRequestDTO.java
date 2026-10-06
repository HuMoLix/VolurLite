package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyAssertionVerifyRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流程票据不能为空")
    private String tl;

    private Integer cid;

    @NotNull(message = "通行密钥凭据不能为空")
    private Map<String, Object> credential;

    private Boolean rememberMe;

}
