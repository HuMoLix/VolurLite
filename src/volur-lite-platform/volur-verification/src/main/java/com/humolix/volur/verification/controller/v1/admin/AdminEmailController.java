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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/emails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailController {

    private final AdminEmailService adminEmailService;

    @GetMapping("/stats")
    public ApiResponseEntity<AdminEmailStatisticsVO> getStatistics() {
        return ApiResponseEntity.ok("获取邮件统计指标成功", adminEmailService.getStatistics());
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<EmailLogVO>> listEmails(AdminEmailQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取邮件记录成功", adminEmailService.listEmails(queryDTO));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<EmailLogVO> getEmailDetail(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取邮件详情成功", adminEmailService.getEmailDetail(id));
    }

    @Syslog(module = "VERIFICATION", value = "管理员手动发送邮件", recordParams = true)
    @PostMapping("/send")
    public ApiResponseEntity<Void> sendManualEmail(
            @Valid @RequestBody AdminSendEmailDTO sendDTO,
            @CurrentUserId(required = false) Long currentUserId) {
        adminEmailService.sendManualEmail(sendDTO, currentUserId);
        return ApiResponseEntity.success("邮件发送任务已提交");
    }

    @Syslog(module = "VERIFICATION", value = "管理员重发邮件", recordParams = true)
    @PostMapping("/{id}/resend")
    public ApiResponseEntity<Void> resendEmail(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long currentUserId) {
        adminEmailService.resendEmail(id, currentUserId);
        return ApiResponseEntity.success("邮件重发任务已提交");
    }

}
