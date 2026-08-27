package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证与账户管理", description = "用户注册、登录、密码找回与重置、邮箱验证等认证相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @Syslog(module = "AUTH", value = "用户注册", logType = "AUTH", recordParams = false)
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ApiResponseEntity.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @Syslog(module = "AUTH", value = "用户登录", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/login")
    public ApiResponse<AuthenticateResponseVO> login(@Valid @RequestBody AuthenticateRequestDTO request) {
        AuthenticateResponseVO response = authService.authenticate(request);
        return ApiResponseEntity.ok("登录成功", response);
    }

    @Operation(summary = "发起找回密码申请")
    @Syslog(module = "AUTH", value = "发起找回密码申请", logType = "AUTH", recordParams = true)
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.processForgotPassword(request);
        return ApiResponseEntity.success("重置密码邮件已发送，请查收");
    }

    @Operation(summary = "根据重置令牌重置密码")
    @Syslog(module = "AUTH", value = "根据令牌重置密码", logType = "AUTH", recordParams = false)
    @PostMapping("/password-resets")
    public ApiResponse<Void> resetPasswordByToken(@Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        authService.resetPasswordByToken(request.getToken(), request.getNewPassword());
        return ApiResponseEntity.success("密码重置成功，请重新登录");
    }

    @Operation(summary = "根据邮箱验证码确认重置密码")
    @Syslog(module = "AUTH", value = "根据邮箱验证码重置密码", logType = "AUTH", recordParams = false)
    @PostMapping("/reset-password-confirm")
    public ApiResponse<Void> resetPasswordConfirm(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ApiResponseEntity.success("密码重置成功");
    }

    @Operation(summary = "发送修改密码邮箱验证码")
    @Syslog(module = "AUTH", value = "发送修改密码邮箱验证码", logType = "AUTH", recordParams = true)
    @PostMapping("/password/send-code")
    public ApiResponse<Void> sendChangePasswordEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendChangePasswordEmail(request);
        return ApiResponseEntity.success("修改密码验证码已发送");
    }

    @Operation(summary = "核验修改密码邮箱验证码并获取令牌")
    @Syslog(module = "AUTH", value = "核验修改密码验证码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/password/verify-code")
    public ApiResponse<String> verifyChangePasswordCode(@Valid @RequestBody VerifyChangePasswordCodeRequestDTO request) {
        String token = authService.verifyChangePasswordCode(request);
        return ApiResponseEntity.ok("验证成功", token);
    }

    @Operation(summary = "修改当前用户密码")
    @Syslog(module = "AUTH", value = "修改当前用户密码", logType = "AUTH", recordParams = false)
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(request);
        return ApiResponseEntity.success("密码修改成功，请重新登录");
    }

    @Operation(summary = "注销当前用户账号")
    @Syslog(module = "AUTH", value = "注销当前用户账号", logType = "AUTH")
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(HttpServletRequest request) {
        authService.deleteAccount(request);
        return ApiResponseEntity.success("账号注销成功");
    }

}
