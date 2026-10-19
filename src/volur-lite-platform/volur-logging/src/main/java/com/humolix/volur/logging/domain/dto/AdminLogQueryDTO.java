package com.humolix.volur.logging.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogQueryDTO {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private String level;

    private String module;

    private Boolean status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endTime;

    private String sortBy = "createdAt";

    private String sortOrder = "desc";

}
