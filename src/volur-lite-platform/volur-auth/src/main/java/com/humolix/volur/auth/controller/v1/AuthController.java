package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.verification.domain.dto.VerifyChangePasswordCodeRequestDTO;
import com.humolix.volur.auth.exception.AuthException;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequestDTO request) throws AuthException {
        authService.register(request);
        return ApiResponseEntity.success("注册成功");
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticateResponseVO> login(@Valid @RequestBody AuthenticateRequestDTO request) throws AuthException {
        AuthenticateResponseVO response = authService.authenticate(request);
        return ApiResponseEntity.ok("登录成功", response);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.processForgotPassword(request);
        return ApiResponseEntity.success("重置密码邮件已发送，请查收");
    }

    @PostMapping("/password-resets")
    public ApiResponse<Void> resetPasswordByToken(@Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        authService.resetPasswordByToken(request.getToken(), request.getNewPassword());
        return ApiResponseEntity.success("密码重置成功，请重新登录");
    }

    @PostMapping("/reset-password-confirm")
    public ApiResponse<Void> resetPasswordConfirm(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ApiResponseEntity.success("密码重置成功");
    }

    @PostMapping("/email/verification")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码已发送至您的邮箱");
    }

    @PostMapping("/email/change-password")
    public ApiResponse<Void> sendChangePasswordEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendChangePasswordEmail(request);
        return ApiResponseEntity.success("修改密码验证码已发送");
    }

    @PostMapping("/password/verify-code")
    public ApiResponse<String> verifyChangePasswordCode(@Valid @RequestBody VerifyChangePasswordCodeRequestDTO request) {
        String token = authService.verifyChangePasswordCode(request);
        return ApiResponseEntity.ok("验证成功", token);
    }

    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(request);
        return ApiResponseEntity.success("密码修改成功，请重新登录");
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(HttpServletRequest request) {
        authService.deleteAccount(request);
        return ApiResponseEntity.success("账号注销成功");
    }

}
