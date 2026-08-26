package com.humolix.volur.challenge.domain.dto.stage.ctf;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfCategoryCreateRequestDTO {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Integer sortOrder;

}
