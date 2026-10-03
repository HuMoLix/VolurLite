package com.humolix.volur.logging.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.domain.dto.AdminLogQueryDTO;
import com.humolix.volur.logging.domain.vo.AdminLogStatisticsVO;
import com.humolix.volur.logging.domain.vo.AdminLogVO;
import com.humolix.volur.logging.service.admin.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public ApiResponseEntity<PageResponse<AdminLogVO>> listLogs(AdminLogQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取系统日志成功", adminLogService.listLogs(queryDTO));
    }

    @GetMapping("/stats")
    public ApiResponseEntity<AdminLogStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取日志统计指标成功", adminLogService.getStatistics());
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<AdminLogVO> getLogDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取日志详情成功", adminLogService.getLogDetail(id));
    }

}
