package com.humolix.volur.system.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.Audit;
import com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import com.humolix.volur.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端系统设置", description = "管理员管理系统设置相关接口")
@RestController
@RequestMapping("/api/v1/admin/system/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "获取所有系统设置列表")
    @GetMapping
    public ApiResponse<List<SystemSettingVO>> getAllSettings() {
        return ApiResponseEntity.ok("获取系统设置列表成功", systemSettingService.getAllSettings());
    }

    @Operation(summary = "单个更新系统设置")
    @Audit(module = "system", action = "update_setting")
    @PostMapping("/update")
    public ApiResponse<Void> updateSetting(@Valid @RequestBody SystemSettingUpdateDTO request) {
        systemSettingService.updateSetting(request);
        return ApiResponseEntity.success("更新系统设置成功");
    }

    @Operation(summary = "批量更新系统设置")
    @Audit(module = "system", action = "batch_update_settings")
    @PostMapping("/batch-update")
    public ApiResponse<Void> batchUpdateSettings(@RequestBody List<@Valid SystemSettingUpdateDTO> requests) {
        systemSettingService.updateSettings(requests);
        return ApiResponseEntity.success("批量更新系统设置成功");
    }

}
