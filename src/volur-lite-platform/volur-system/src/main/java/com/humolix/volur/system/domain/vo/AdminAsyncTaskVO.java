package com.humolix.volur.system.domain.vo;

import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
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
public class AdminAsyncTaskVO implements Serializable {

    private Long id;

    private String taskName;

    private AsyncTaskType taskType;

    private String taskTypeDesc;

    private AsyncTaskStatus status;

    private String statusDesc;

    @Builder.Default
    private Integer progress = 0;

    private String statusMessage;

    private String errorMessage;

    private String parameters;

    private String resultData;

    private Long operatorId;

    private String operatorName;

    private Instant createdAt;

    private Instant startedAt;

    private Instant finishedAt;

    private Long executionDurationMs;

}
