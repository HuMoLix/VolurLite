package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminUserPageRequestDTO extends PageRequestDTO {

    @Trimmed
    private String keyword;

    @Trimmed
    private String role;

    private Boolean status;

    private Instant startTime;

    private Instant endTime;

}
