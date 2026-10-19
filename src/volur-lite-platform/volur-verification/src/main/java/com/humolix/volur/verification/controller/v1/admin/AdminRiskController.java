package com.humolix.volur.verification.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.AdminIpRuleCreateDTO;
import com.humolix.volur.verification.domain.dto.AdminIpRuleQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO;
import com.humolix.volur.verification.domain.vo.*;
import com.humolix.volur.verification.service.admin.AdminRiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskController {

    private final AdminRiskService adminRiskService;

    @GetMapping("/settings")
    public ApiResponseEntity<AdminRiskSettingsVO> getSettings() {
        return ApiResponseEntity.ok("获取风控基础设置成功", adminRiskService.getSettings());
    }

    @Syslog(module = "VERIFICATION", value = "更新风控全局基础设置", recordParams = true)
    @PutMapping("/settings")
    public ApiResponseEntity<Void> updateSettings(
            @Valid @RequestBody AdminRiskSettingsDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.updateSettings(dto, currentUserId);
        return ApiResponseEntity.success("更新风控基础设置成功");
    }

    @GetMapping("/policy")
    public ApiResponseEntity<AdminRiskPolicyVO> getPolicy() {
        return ApiResponseEntity.ok("获取风控策略配置成功", adminRiskService.getPolicy());
    }

    @Syslog(module = "VERIFICATION", value = "更新风控策略配置", recordParams = true)
    @PutMapping("/policy")
    public ApiResponseEntity<Void> updatePolicy(
            @Valid @RequestBody AdminRiskPolicyDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.updatePolicy(dto, currentUserId);
        return ApiResponseEntity.success("更新风控策略配置成功");
    }

    @GetMapping("/ip-rules")
    public ApiResponseEntity<PageResponse<AdminIpRuleVO>> listIpRules(AdminIpRuleQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取 IP 规则列表成功", adminRiskService.listIpRules(queryDTO));
    }

    @Syslog(module = "VERIFICATION", value = "创建 IP 治理规则", recordParams = true)
    @PostMapping("/ip-rules")
    public ApiResponseEntity<Void> createIpRule(
            @Valid @RequestBody AdminIpRuleCreateDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        String operatorIp = RequestUtil.getClientIp();
        adminRiskService.createIpRule(dto, currentUserId, "ADMIN", operatorIp);
        return ApiResponseEntity.success("创建 IP 规则成功");
    }

    @Syslog(module = "VERIFICATION", value = "撤销/解封 IP 规则", recordParams = true)
    @PostMapping("/ip-rules/{id}/revoke")
    public ApiResponseEntity<Void> revokeIpRule(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.revokeIpRule(id, currentUserId, "ADMIN");
        return ApiResponseEntity.success("撤销 IP 规则成功");
    }

    @Syslog(module = "VERIFICATION", value = "删除 IP 规则", recordParams = true)
    @DeleteMapping("/ip-rules/{id}")
    public ApiResponseEntity<Void> deleteIpRule(@PathVariable Long id) {
        adminRiskService.deleteIpRule(id);
        return ApiResponseEntity.success("删除 IP 规则成功");
    }

    @GetMapping("/ip-rules/diagnose")
    public ApiResponseEntity<AdminIpDiagnosisVO> diagnoseIp(@RequestParam String ip) {
        return ApiResponseEntity.ok("诊断 IP 成功", adminRiskService.diagnoseIp(ip));
    }

    @GetMapping("/stats")
    public ApiResponseEntity<AdminRiskStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取风控统计数据成功", adminRiskService.getStatistics());
    }

}
