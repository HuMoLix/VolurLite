package com.humolix.volur.system.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.system.domain.dto.AdminAsyncTaskPageRequestDTO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskStatisticsVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;
import com.humolix.volur.system.service.admin.AdminAsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端异步任务中心", description = "后台长时间链路异步任务执行状态、进度轮询与管控接口")
@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAsyncTaskController {

    private final AdminAsyncTaskService adminAsyncTaskService;

    @Operation(summary = "获取异步任务统计看板数据")
    @GetMapping("/statistics")
    public ApiResponse<AdminAsyncTaskStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取任务统计数据成功", adminAsyncTaskService.getStatistics());
    }

    @Operation(summary = "分页获取异步任务列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminAsyncTaskVO>> listTasks(AdminAsyncTaskPageRequestDTO request) {
        return ApiResponseEntity.ok("获取任务列表成功", adminAsyncTaskService.listTasks(request));
    }

    @Operation(summary = "获取指定异步任务详细信息")
    @GetMapping("/{id}")
    public ApiResponse<AdminAsyncTaskVO> getTaskDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取任务详情成功", adminAsyncTaskService.getTaskDetail(id));
    }

    @Operation(summary = "取消正在排队或执行中的异步任务")
    @Syslog(module = "SYSTEM", value = "取消异步任务", recordParams = true)
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelTask(@PathVariable Long id) {
        adminAsyncTaskService.cancelTask(id);
        return ApiResponseEntity.success("任务已成功申请取消");
    }

    @Operation(summary = "删除指定任务记录")
    @Syslog(module = "SYSTEM", value = "删除任务记录", recordParams = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        adminAsyncTaskService.deleteTask(id);
        return ApiResponseEntity.success("任务记录已成功删除");
    }

    @Operation(summary = "清理已结束的历史任务记录")
    @Syslog(module = "SYSTEM", value = "清理历史任务记录", recordParams = true)
    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(@RequestParam(required = false) String status) {
        adminAsyncTaskService.clearHistory(status);
        return ApiResponseEntity.success("任务历史记录已清理完成");
    }

}
