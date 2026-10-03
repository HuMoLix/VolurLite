package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpCompleteRequestDTO {

    @NotBlank(message = "会话句柄不能为空")
    private String dsh;

    @NotBlank(message = "流转锁票据不能为空")
    private String tl;

    @NotNull(message = "步骤序号不能为空")
    private Integer cid;

    private Boolean skipOptions = true;

    private List<String> selectedOptions;

}
