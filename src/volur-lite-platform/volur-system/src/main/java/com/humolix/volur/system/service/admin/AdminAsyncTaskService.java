package com.humolix.volur.system.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.system.domain.dto.AdminAsyncTaskPageRequestDTO;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskStatisticsVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;

import java.util.function.Consumer;

public interface AdminAsyncTaskService {

    AdminAsyncTaskSubmitVO submitTask(
            AsyncTaskType taskType,
            String taskName,
            Long operatorId,
            String operatorName,
            Object parameters,
            Consumer<AsyncTaskContext> taskRunnable
    );

    PageResponse<AdminAsyncTaskVO> listTasks(AdminAsyncTaskPageRequestDTO request);

    AdminAsyncTaskStatisticsVO getStatistics();

    AdminAsyncTaskVO getTaskDetail(Long taskId);

    void cancelTask(Long taskId);

    void deleteTask(Long taskId);

    void clearHistory(String status);

    void updateProgress(Long taskId, int progress, String statusMessage);

    void completeTask(Long taskId, Object resultData);

    void failTask(Long taskId, String errorMessage);

}
