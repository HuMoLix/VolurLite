package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "邮件服务管理", description = "发送注册验证码及 SMTP 连接探活接口")
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "发送注册邮箱验证码")
    @Syslog(module = "VERIFICATION", value = "发送注册邮箱验证码", logType = "AUTH", recordParams = true)
    @PostMapping("/captcha/register")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        emailService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码发送成功");
    }

    @Operation(summary = "测试 SMTP 邮件服务器连通性")
    @Syslog(module = "VERIFICATION", value = "测试 SMTP 邮件连接", logType = "SYSTEM", recordParams = false)
    @PostMapping("/test")
    public ApiResponse<Void> testSmtpConnection() {
        emailService.testSmtpConnection();
        return ApiResponseEntity.success("邮件服务连接测试成功");
    }

}
