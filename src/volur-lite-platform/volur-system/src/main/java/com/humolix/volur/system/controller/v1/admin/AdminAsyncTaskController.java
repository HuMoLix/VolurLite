package com.humolix.volur.system.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.system.domain.dto.AdminAsyncTaskPageRequestDTO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskStatisticsVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;
import com.humolix.volur.system.service.admin.AdminAsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAsyncTaskController {

    private final AdminAsyncTaskService adminAsyncTaskService;

    @GetMapping("/stats")
    public ApiResponseEntity<AdminAsyncTaskStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取任务统计数据成功", adminAsyncTaskService.getStatistics());
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminAsyncTaskVO>> listTasks(AdminAsyncTaskPageRequestDTO request) {
        return ApiResponseEntity.ok("获取任务列表成功", adminAsyncTaskService.listTasks(request));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminAsyncTaskVO> getTaskDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取任务详情成功", adminAsyncTaskService.getTaskDetail(id));
    }

    @Syslog(module = "SYSTEM", value = "取消异步任务", recordParams = true)
    @PostMapping("/{id}/cancel")
    public ApiResponseEntity<Void> cancelTask(@PathVariable Long id) {
        adminAsyncTaskService.cancelTask(id);
        return ApiResponseEntity.success("任务已成功申请取消");
    }

    @Syslog(module = "SYSTEM", value = "删除任务记录", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteTask(@PathVariable Long id) {
        adminAsyncTaskService.deleteTask(id);
        return ApiResponseEntity.success("任务记录已成功删除");
    }

    @Syslog(module = "SYSTEM", value = "清理历史任务记录", recordParams = true)
    @DeleteMapping("/history")
    public ApiResponseEntity<Void> clearHistory(@RequestParam(required = false) String status) {
        adminAsyncTaskService.clearHistory(status);
        return ApiResponseEntity.success("任务历史记录已清理完成");
    }

}
