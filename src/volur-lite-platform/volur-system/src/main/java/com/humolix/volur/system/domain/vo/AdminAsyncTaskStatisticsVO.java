package com.humolix.volur.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAsyncTaskStatisticsVO implements Serializable {

    @Builder.Default
    private long totalCount = 0;

    @Builder.Default
    private long runningCount = 0;

    @Builder.Default
    private long pendingCount = 0;

    @Builder.Default
    private long successCount = 0;

    @Builder.Default
    private long failedCount = 0;

}
