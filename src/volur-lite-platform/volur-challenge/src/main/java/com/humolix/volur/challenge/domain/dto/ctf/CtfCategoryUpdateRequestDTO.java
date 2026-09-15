package com.humolix.volur.challenge.domain.dto.ctf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfCategoryUpdateRequestDTO {

    private String name;

    private String description;

    private Integer sortOrder;

}
