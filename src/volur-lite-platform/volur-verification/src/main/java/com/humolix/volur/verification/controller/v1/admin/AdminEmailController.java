package com.humolix.volur.verification.controller.v1.admin;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.AdminEmailQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminSendEmailDTO;
import com.humolix.volur.verification.domain.vo.AdminEmailStatisticsVO;
import com.humolix.volur.verification.domain.vo.EmailLogVO;
import com.humolix.volur.verification.service.admin.AdminEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端邮件管理", description = "管理员查看系统发信流水、发信统计指标、邮件详情及手动发送邮件接口")
@RestController
@RequestMapping("/api/v1/admin/emails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailController {

    private final AdminEmailService adminEmailService;

    @Operation(summary = "获取邮件发信统计指标")
    @GetMapping("/statistics")
    public ApiResponse<AdminEmailStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取邮件统计指标成功", adminEmailService.getStatistics());
    }

    @Operation(summary = "分页查询邮件发送记录列表")
    @GetMapping
    public ApiResponse<PageResponse<EmailLogVO>> listEmails(AdminEmailQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取邮件记录成功", adminEmailService.listEmails(queryDTO));
    }

    @Operation(summary = "获取指定邮件详细内容")
    @GetMapping("/{id}")
    public ApiResponse<EmailLogVO> getEmailDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取邮件详情成功", adminEmailService.getEmailDetail(id));
    }

    @Operation(summary = "管理员手动发送邮件")
    @Syslog(module = "VERIFICATION", value = "管理员手动发送邮件", recordParams = true)
    @PostMapping("/send")
    public ApiResponse<Void> sendManualEmail(
            @Valid @RequestBody AdminSendEmailDTO sendDTO,
            @CurrentUserId(required = false) Long currentUserId) {
        adminEmailService.sendManualEmail(sendDTO, currentUserId);
        return ApiResponseEntity.success("邮件发送成功");
    }

    @Operation(summary = "重发指定邮件")
    @Syslog(module = "VERIFICATION", value = "管理员重发邮件", recordParams = true)
    @PostMapping("/{id}/resend")
    public ApiResponse<Void> resendEmail(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long currentUserId) {
        adminEmailService.resendEmail(id, currentUserId);
        return ApiResponseEntity.success("邮件重发成功");
    }

}
