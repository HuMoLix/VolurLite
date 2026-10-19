package com.humolix.volur.challenge.domain.vo.ctf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CtfCategoryVO implements Serializable {

    private Long id;
    private String name;
    private String icon;
    private String description;
    private Integer sortOrder;
    private Instant createdAt;

}
