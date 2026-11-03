package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.*;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Deprecated(since = "1.0.0", forRemoval = true)
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Syslog(module = "AUTH", value = "用户注册", logType = "AUTH", recordParams = false)
    @PostMapping("/register")
    public ApiResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ApiResponseEntity.success("注册成功");
    }

    @Syslog(module = "AUTH", value = "用户登录", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/login")
    public ApiResponseEntity<AuthenticateResponseVO> login(
            @Valid @RequestBody AuthenticateRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthenticateResponseVO response = authService.authenticate(request);
        authCookieService.writeAuthCookies(httpRequest, httpResponse, response.getAccessToken(), response.getRefreshToken(), Boolean.TRUE.equals(request.getRememberMe()));
        return ApiResponseEntity.ok("登录成功", response);
    }

    @Syslog(module = "AUTH", value = "发起找回密码申请", logType = "AUTH", recordParams = true)
    @PostMapping("/forgot-password")
    public ApiResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.processForgotPassword(request);
        return ApiResponseEntity.success("重置密码邮件已发送，请查收");
    }

    @Syslog(module = "AUTH", value = "根据令牌重置密码", logType = "AUTH", recordParams = false)
    @PostMapping("/password-resets")
    public ApiResponseEntity<Void> resetPasswordByToken(@Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        authService.resetPasswordByToken(request.getToken(), request.getNewPassword());
        return ApiResponseEntity.success("密码重置成功，请重新登录");
    }

    @Syslog(module = "AUTH", value = "根据邮箱验证码重置密码", logType = "AUTH", recordParams = false)
    @PostMapping("/reset-password-confirm")
    public ApiResponseEntity<Void> resetPasswordConfirm(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ApiResponseEntity.success("密码重置成功");
    }

    @Syslog(module = "AUTH", value = "发送修改密码邮箱验证码", logType = "AUTH", recordParams = true)
    @PostMapping("/password/send-code")
    public ApiResponseEntity<Void> sendChangePasswordEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        authService.sendChangePasswordEmail(request);
        return ApiResponseEntity.success("修改密码验证码已发送");
    }

    @Syslog(module = "AUTH", value = "核验修改密码验证码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/password/verify-code")
    public ApiResponseEntity<String> verifyChangePasswordCode(@Valid @RequestBody VerifyChangePasswordCodeRequestDTO request) {
        String token = authService.verifyChangePasswordCode(request);
        return ApiResponseEntity.ok("验证成功", token);
    }

    @Syslog(module = "AUTH", value = "修改当前用户密码", logType = "AUTH", recordParams = false)
    @PostMapping("/password/change")
    public ApiResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(request);
        return ApiResponseEntity.success("密码修改成功，请重新登录");
    }

    @Syslog(module = "AUTH", value = "发送注销账号邮箱验证码", logType = "AUTH", recordParams = true)
    @PostMapping("/account/send-code")
    public ApiResponseEntity<Void> sendDeleteAccountEmail(@Valid @RequestBody(required = false) EmailCaptchaRequestDTO request) {
        authService.sendDeleteAccountEmail(request);
        return ApiResponseEntity.success("注销账号验证码已发送");
    }

    @Syslog(module = "AUTH", value = "注销当前用户账号", logType = "AUTH")
    @PostMapping("/account/delete")
    public ApiResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequestDTO request,
            HttpServletRequest httpRequest) {
        authService.deleteAccount(request, httpRequest);
        return ApiResponseEntity.success("账号注销成功");
    }

}
