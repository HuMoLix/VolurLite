package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
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

    @PostMapping("/captcha/register")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        emailService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码发送成功");
    }

    @PostMapping("/captcha/change-password")
    public ApiResponse<Void> sendChangePasswordEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        emailService.sendChangePasswordEmail(request);
        return ApiResponseEntity.success("验证码已发送至您的邮箱，请查收");
    }

    @PostMapping("/captcha/verify")
    public ApiResponse<String> verifyChangePasswordCode(@Valid @RequestBody VerifyChangePasswordCodeRequestDTO request) {
        String token = emailService.verifyChangePasswordCode(request);
        return ApiResponseEntity.ok("验证码验证成功", token);
    }

    @PostMapping("/test")
    public ApiResponse<Void> testSmtpConnection() {
        emailService.testSmtpConnection();
        return ApiResponseEntity.success("邮件服务连接测试成功");
    }

}
