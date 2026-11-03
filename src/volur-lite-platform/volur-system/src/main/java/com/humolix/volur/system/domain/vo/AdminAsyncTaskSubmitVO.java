package com.humolix.volur.system.domain.vo;

import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAsyncTaskSubmitVO implements Serializable {

    private Long taskId;

    private String taskName;

    private AsyncTaskType taskType;

    private AsyncTaskStatus status;

}
