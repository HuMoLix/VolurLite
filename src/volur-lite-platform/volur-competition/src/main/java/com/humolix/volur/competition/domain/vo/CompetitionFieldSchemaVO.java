package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.FieldType;
import com.humolix.volur.competition.domain.enums.FillScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionFieldSchemaVO implements Serializable {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private FieldType fieldType;
    private Boolean isRequired;
    private String regexPattern;
    private String selectOptions;
    private FillScope fillScope;
    private Integer sortOrder;

}
