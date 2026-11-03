package com.humolix.volur.logging.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogStatisticsVO implements Serializable {

    @Builder.Default
    private long totalLogs = 0L;

    @Builder.Default
    private long authLogs = 0L;

    @Builder.Default
    private long securityLogs = 0L;

    @Builder.Default
    private long errorLogs = 0L;

}
