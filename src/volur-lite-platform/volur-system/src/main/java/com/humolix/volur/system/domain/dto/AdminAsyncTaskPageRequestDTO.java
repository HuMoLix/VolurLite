package com.humolix.volur.system.domain.dto;

import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminAsyncTaskPageRequestDTO extends PageRequestDTO {

    private String keyword;

    private AsyncTaskType taskType;

    private AsyncTaskStatus status;

    private Instant startTime;

    private Instant endTime;

}
