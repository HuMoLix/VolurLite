package com.humolix.volur.challenge.domain.dto.theory;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryBankUpdateRequestDTO {

    @Size(max = 128, message = "题库名称长度不能超过128个字符")
    private String name;

    private String description;

    @Size(max = 64, message = "分类名称长度不能超过64个字符")
    private String category;

}
