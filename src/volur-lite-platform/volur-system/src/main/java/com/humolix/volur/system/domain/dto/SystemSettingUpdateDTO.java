package com.humolix.volur.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingUpdateDTO {

    @NotBlank(message = "配置键名不能为空")
    private String key;

    @NotNull(message = "配置值不能为 null")
    private String value;

}
