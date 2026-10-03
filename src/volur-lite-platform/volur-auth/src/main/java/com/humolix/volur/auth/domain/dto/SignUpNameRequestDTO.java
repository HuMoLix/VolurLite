package com.humolix.volur.auth.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpNameRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    private String tl;

    private Integer cid = 0;

    @NotBlank(message = "用户名凭据不能为空")
    @Trimmed
    private String username;

}
