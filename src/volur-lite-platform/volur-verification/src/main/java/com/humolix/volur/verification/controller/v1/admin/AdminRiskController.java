package com.humolix.volur.verification.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.framework.security.util.RequestUtil;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.AdminIpRuleCreateDTO;
import com.humolix.volur.verification.domain.dto.AdminIpRuleQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO;
import com.humolix.volur.verification.domain.vo.AdminIpDiagnosisVO;
import com.humolix.volur.verification.domain.vo.AdminIpRuleVO;
import com.humolix.volur.verification.domain.vo.AdminRiskPolicyVO;
import com.humolix.volur.verification.domain.vo.AdminRiskSettingsVO;
import com.humolix.volur.verification.domain.vo.AdminRiskStatisticsVO;
import com.humolix.volur.verification.service.admin.AdminRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端风控管理", description = "管理员配置风控开关、防御策略阈值、IP封禁黑白名单及IP实时诊断探针")
@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskController {

    private final AdminRiskService adminRiskService;

    @Operation(summary = "获取风控全局基础设置")
    @GetMapping("/settings")
    public ApiResponse<AdminRiskSettingsVO> getSettings() {
        return ApiResponseEntity.ok("获取风控基础设置成功", adminRiskService.getSettings());
    }

    @Operation(summary = "更新风控全局基础设置")
    @Syslog(module = "VERIFICATION", value = "更新风控全局基础设置", recordParams = true)
    @PutMapping("/settings")
    public ApiResponse<Void> updateSettings(
            @Valid @RequestBody AdminRiskSettingsDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.updateSettings(dto, currentUserId);
        return ApiResponseEntity.success("更新风控基础设置成功");
    }

    @Operation(summary = "获取风控策略与防御阈值配置")
    @GetMapping("/policy")
    public ApiResponse<AdminRiskPolicyVO> getPolicy() {
        return ApiResponseEntity.ok("获取风控策略配置成功", adminRiskService.getPolicy());
    }

    @Operation(summary = "更新风控策略与防御阈值配置")
    @Syslog(module = "VERIFICATION", value = "更新风控策略配置", recordParams = true)
    @PutMapping("/policy")
    public ApiResponse<Void> updatePolicy(
            @Valid @RequestBody AdminRiskPolicyDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.updatePolicy(dto, currentUserId);
        return ApiResponseEntity.success("更新风控策略配置成功");
    }

    @Operation(summary = "分页查询 IP 规则列表")
    @GetMapping("/ip-rules")
    public ApiResponse<PageResponse<AdminIpRuleVO>> listIpRules(AdminIpRuleQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取 IP 规则列表成功", adminRiskService.listIpRules(queryDTO));
    }

    @Operation(summary = "手动添加 IP 规则 (封禁或白名单)")
    @Syslog(module = "VERIFICATION", value = "创建 IP 治理规则", recordParams = true)
    @PostMapping("/ip-rules")
    public ApiResponse<Void> createIpRule(
            @Valid @RequestBody AdminIpRuleCreateDTO dto,
            @CurrentUserId(required = false) Long currentUserId) {
        String operatorIp = RequestUtil.getClientIp();
        adminRiskService.createIpRule(dto, currentUserId, "ADMIN", operatorIp);
        return ApiResponseEntity.success("创建 IP 规则成功");
    }

    @Operation(summary = "撤销/解封指定 IP 规则")
    @Syslog(module = "VERIFICATION", value = "撤销/解封 IP 规则", recordParams = true)
    @PostMapping("/ip-rules/{id}/revoke")
    public ApiResponse<Void> revokeIpRule(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long currentUserId) {
        adminRiskService.revokeIpRule(id, currentUserId, "ADMIN");
        return ApiResponseEntity.success("撤销 IP 规则成功");
    }

    @Operation(summary = "删除指定 IP 规则")
    @Syslog(module = "VERIFICATION", value = "删除 IP 规则", recordParams = true)
    @DeleteMapping("/ip-rules/{id}")
    public ApiResponse<Void> deleteIpRule(@PathVariable Long id) {
        adminRiskService.deleteIpRule(id);
        return ApiResponseEntity.success("删除 IP 规则成功");
    }

    @Operation(summary = "IP 实时风险诊断探针")
    @GetMapping("/ip-rules/diagnose")
    public ApiResponse<AdminIpDiagnosisVO> diagnoseIp(@RequestParam String ip) {
        return ApiResponseEntity.ok("诊断 IP 成功", adminRiskService.diagnoseIp(ip));
    }

    @Operation(summary = "获取风控与 IP 治理统计指标")
    @GetMapping("/stats")
    public ApiResponse<AdminRiskStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取风控统计数据成功", adminRiskService.getStatistics());
    }

}
