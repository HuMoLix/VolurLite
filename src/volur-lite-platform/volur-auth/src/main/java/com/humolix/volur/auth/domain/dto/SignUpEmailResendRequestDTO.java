package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpEmailResendRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转锁票据不能为空")
    private String tl;

    @NotNull(message = "步骤序号不能为空")
    private Integer cid;

}
