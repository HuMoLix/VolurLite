package com.humolix.volur.security.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitSessionRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "流程名称不能为空")
    @Trimmed
    private String flowName;

    @NotBlank(message = "流程入口不能为空")
    @Trimmed
    private String flowEntry;

    @NotBlank(message = "设备指纹不能为空")
    @Trimmed
    private String fp;

    @NotNull(message = "客户端时间戳不能为空")
    private Long t;

    @NotBlank(message = "设备参数密文不能为空")
    @Trimmed
    private String data;

}
