package com.humolix.volur.system.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO;
import com.humolix.volur.system.domain.vo.AdminSystemOverviewVO;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping("/overview")
    public ApiResponseEntity<AdminSystemOverviewVO> getSystemOverview() {
        return ApiResponseEntity.ok("获取系统概览成功", systemSettingService.getSystemOverview());
    }

    @GetMapping
    public ApiResponseEntity<List<SystemSettingVO>> getAllSettings() {
        return ApiResponseEntity.ok("获取系统设置列表成功", systemSettingService.getAllSettings());
    }

    @Syslog(module = "SYSTEM", value = "单个更新系统设置", recordParams = true)
    @PostMapping("/update")
    public ApiResponseEntity<Void> updateSetting(@Valid @RequestBody SystemSettingUpdateDTO request) {
        systemSettingService.updateSetting(request);
        return ApiResponseEntity.success("更新系统设置成功");
    }

    @Syslog(module = "SYSTEM", value = "批量更新系统设置", recordParams = true)
    @PostMapping("/batch-update")
    public ApiResponseEntity<Void> batchUpdateSettings(@RequestBody List<@Valid SystemSettingUpdateDTO> requests) {
        systemSettingService.updateSettings(requests);
        return ApiResponseEntity.success("批量更新系统设置成功");
    }

}
