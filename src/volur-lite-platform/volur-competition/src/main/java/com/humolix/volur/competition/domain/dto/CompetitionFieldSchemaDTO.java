package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.FieldType;
import com.humolix.volur.competition.domain.enums.FillScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionFieldSchemaDTO {

    @NotBlank(message = "字段标识不能为空")
    private String fieldKey;

    @NotBlank(message = "字段标签不能为空")
    private String fieldLabel;

    @NotNull(message = "字段类型不能为空")
    private FieldType fieldType;

    private Boolean isRequired;

    private String regexPattern;

    private String selectOptions;

    private FillScope fillScope;

    private Integer sortOrder;

}
