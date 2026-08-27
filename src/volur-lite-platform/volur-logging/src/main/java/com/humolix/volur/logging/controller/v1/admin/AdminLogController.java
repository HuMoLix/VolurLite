package com.humolix.volur.logging.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.domain.dto.AdminLogQueryDTO;
import com.humolix.volur.logging.domain.vo.AdminLogStatisticsVO;
import com.humolix.volur.logging.domain.vo.AdminLogVO;
import com.humolix.volur.logging.service.admin.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端系统审计日志", description = "管理员查看系统运行流水、审计日志与统计指标接口")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @Operation(summary = "分页查询系统审计日志列表")
    @GetMapping
    public ApiResponse<PageResponse<AdminLogVO>> listLogs(AdminLogQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取系统日志成功", adminLogService.listLogs(queryDTO));
    }

    @Operation(summary = "获取系统日志统计指标")
    @GetMapping("/statistics")
    public ApiResponse<AdminLogStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取日志统计指标成功", adminLogService.getStatistics());
    }

    @Operation(summary = "获取指定审计日志详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminLogVO> getLogDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取日志详情成功", adminLogService.getLogDetail(id));
    }

}
