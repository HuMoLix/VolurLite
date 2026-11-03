package com.humolix.volur.security.domain.vo;

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
public class TotpStatusVO implements Serializable {

    @Builder.Default
    private Boolean enabled = false;

    private Instant updatedAt;

}
