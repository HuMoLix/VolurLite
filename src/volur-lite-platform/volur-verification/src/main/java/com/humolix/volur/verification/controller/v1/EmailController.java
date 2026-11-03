package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Syslog(module = "VERIFICATION", value = "发送注册邮箱验证码", logType = "AUTH", recordParams = true)
    @PostMapping("/captcha/register")
    public ApiResponseEntity<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        emailService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码发送成功");
    }

    @Syslog(module = "VERIFICATION", value = "测试 SMTP 邮件连接", logType = "SYSTEM", recordParams = false)
    @PostMapping("/test")
    public ApiResponseEntity<Void> testSmtpConnection() {
        emailService.testSmtpConnection();
        return ApiResponseEntity.success("邮件服务连接测试成功");
    }

}
