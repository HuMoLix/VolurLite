package com.humolix.volur.verification.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminEmailQueryDTO {

    private Integer page = 1;

    private Integer size = 20;

    private String sortBy;

    private String sortOrder;

    private String keyword;

    private String type;

    private String status;

    private Instant startTime;

    private Instant endTime;

}
