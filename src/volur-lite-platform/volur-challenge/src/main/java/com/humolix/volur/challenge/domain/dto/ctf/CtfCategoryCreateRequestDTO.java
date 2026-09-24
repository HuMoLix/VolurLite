package com.humolix.volur.challenge.domain.dto.ctf;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfCategoryCreateRequestDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过64个字符")
    @Trimmed
    private String name;

    @Size(max = 64, message = "图标标识长度不能超过64个字符")
    @Trimmed
    private String icon;

    private String description;

    private Integer sortOrder;

}
