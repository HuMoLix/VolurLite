package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerStatisticsVO {

    @Builder.Default
    private long totalInstances = 0L;

    @Builder.Default
    private long runningCount = 0L;

    @Builder.Default
    private long startingCount = 0L;

    @Builder.Default
    private long failedCount = 0L;

    @Builder.Default
    private long todayCreatedCount = 0L;

    private String activeContainerType;

}
