package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.auth.exception.AuthException;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
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
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO request) throws AuthException {
        authService.register(request);
        return ApiResponseEntity.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<AuthenticateResponseVO> login(@Valid @RequestBody AuthenticateRequestDTO request) throws AuthException {
        AuthenticateResponseVO response = authService.authenticate(request);
        return ApiResponseEntity.ok("登录成功", response);
    }

    @Operation(summary = "发起找回密码申请")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.processForgotPassword(request);
        return ApiResponseEntity.success("重置密码邮件已发送，请查收");
    }

    @Operation(summary = "根据重置令牌重置密码")
    @PostMapping("/password-resets")
    public ApiResponse<Void> resetPasswordByToken(@Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        authService.resetPasswordByToken(request.getToken(), request.getNewPassword());
        return ApiResponseEntity.success("密码重置成功，请重新登录");
    }

    @Operation(summary = "根据邮箱验证码确认重置密码")
    @PostMapping("/reset-password-confirm")
    public ApiResponse<Void> resetPasswordConfirm(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ApiResponseEntity.success("密码重置成功");
    }

    @Operation(summary = "发送注册/绑定邮箱验证码")
    @PostMapping("/email/verification")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码已发送至您的邮箱");
    }

    @Operation(summary = "发送修改密码邮箱验证码")
    @PostMapping("/email/change-password")
    public ApiResponse<Void> sendChangePasswordEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendChangePasswordEmail(request);
        return ApiResponseEntity.success("修改密码验证码已发送");
    }

    @Operation(summary = "核验修改密码邮箱验证码并获取令牌")
    @PostMapping("/password/verify-code")
    public ApiResponse<String> verifyChangePasswordCode(@Valid @RequestBody VerifyChangePasswordCodeRequestDTO request) {
        String token = authService.verifyChangePasswordCode(request);
        return ApiResponseEntity.ok("验证成功", token);
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(request);
        return ApiResponseEntity.success("密码修改成功，请重新登录");
    }

    @Operation(summary = "注销当前用户账号")
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(HttpServletRequest request) {
        authService.deleteAccount(request);
        return ApiResponseEntity.success("账号注销成功");
    }

}
