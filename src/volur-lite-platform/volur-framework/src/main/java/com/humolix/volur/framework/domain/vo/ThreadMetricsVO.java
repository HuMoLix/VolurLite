package com.humolix.volur.framework.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadMetricsVO {

    @Builder.Default
    private Boolean virtualThreadsEnabled = true;

    private Long activeVirtualTasks;

    private Long completedVirtualTasks;

    private Long rejectedVirtualTasks;

    private Integer availableIoPermits;

    private Integer maxConcurrentVirtualTasks;

    private Integer computeCorePoolSize;

    private Integer computeMaxPoolSize;

    private Integer computeActiveCount;

    private Integer computeQueueSize;

    private Long computeCompletedTaskCount;

    private Integer availableProcessors;

}
