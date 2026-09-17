package com.humolix.volur.challenge.domain.dto.ctf;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfCategoryUpdateRequestDTO {

    @Size(max = 64, message = "分类名称长度不能超过64个字符")
    private String name;

    @Size(max = 64, message = "图标标识长度不能超过64个字符")
    private String icon;

    private String description;

    private Integer sortOrder;

}
