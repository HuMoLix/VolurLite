package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryEmailOtpRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoveryResetRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.PasswordRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoveryEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.vo.PasswordRecoveryOptionsVO;
import com.humolix.volur.auth.domain.vo.RecoveryFlowResponseVO;
import com.humolix.volur.auth.domain.vo.UsernameRecoveryOptionsVO;
import com.humolix.volur.auth.service.RecoveryService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;

    @Syslog(module = "AUTH", value = "获取找回用户名方式选项", logType = "AUTH", recordParams = false)
    @GetMapping("/username/options")
    public ApiResponseEntity<UsernameRecoveryOptionsVO> getUsernameRecoveryOptions() {
        UsernameRecoveryOptionsVO response = recoveryService.getUsernameRecoveryOptions();
        return ApiResponseEntity.ok("获取找回用户名选项成功", response);
    }

    @Syslog(module = "AUTH", value = "选择找回用户名方式并初始化安全挑战", logType = "AUTH", recordParams = false)
    @PostMapping("/username/select")
    public ApiResponseEntity<RecoveryFlowResponseVO> selectRecoveryMethod(
            @Valid @RequestBody UsernameRecoverySelectRequestDTO request) {
        RecoveryFlowResponseVO response = recoveryService.selectRecoveryMethod(request);
        return ApiResponseEntity.ok("选择找回方式成功，进入安全验证", response);
    }

    @Syslog(module = "AUTH", value = "提交找回用户名邮箱", logType = "AUTH", recordParams = false)
    @PostMapping("/username/email")
    public ApiResponseEntity<RecoveryFlowResponseVO> submitRecoveryEmail(
            @Valid @RequestBody UsernameRecoveryEmailRequestDTO request) {
        RecoveryFlowResponseVO response = recoveryService.submitRecoveryEmail(request);
        return ApiResponseEntity.ok("找回用户名邮件已发送", response);
    }

    @Syslog(module = "AUTH", value = "获取找回密码方式选项", logType = "AUTH", recordParams = false)
    @GetMapping("/password/options")
    public ApiResponseEntity<PasswordRecoveryOptionsVO> getPasswordRecoveryOptions(
            @RequestParam("dsh") String dsh) {
        PasswordRecoveryOptionsVO response = recoveryService.getPasswordRecoveryOptions(dsh);
        return ApiResponseEntity.ok("获取找回密码选项成功", response);
    }

    @Syslog(module = "AUTH", value = "选择找回密码方式并初始化安全挑战", logType = "AUTH", recordParams = false)
    @PostMapping("/password/select")
    public ApiResponseEntity<RecoveryFlowResponseVO> selectPasswordRecoveryMethod(
            @Valid @RequestBody PasswordRecoverySelectRequestDTO request) {
        RecoveryFlowResponseVO response = recoveryService.selectPasswordRecoveryMethod(request);
        return ApiResponseEntity.ok("选择找回方式成功，进入安全验证", response);
    }

    @Syslog(module = "AUTH", value = "发送找回密码邮箱验证码", logType = "AUTH", recordParams = false)
    @PostMapping("/password/emailotp/send")
    public ApiResponseEntity<Void> sendPasswordRecoveryEmailOtp(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        recoveryService.sendPasswordRecoveryEmailOtp(dsh, tl);
        return ApiResponseEntity.success("找回密码邮箱验证码已发送");
    }

    @Syslog(module = "AUTH", value = "核销找回密码邮箱验证码", logType = "AUTH", recordParams = false)
    @PostMapping("/password/emailotp/verify")
    public ApiResponseEntity<RecoveryFlowResponseVO> verifyPasswordRecoveryEmailOtp(
            @Valid @RequestBody PasswordRecoveryEmailOtpRequestDTO request) {
        RecoveryFlowResponseVO response = recoveryService.verifyPasswordRecoveryEmailOtp(request);
        return ApiResponseEntity.ok("邮箱验证码核销成功，请设置新密码", response);
    }

    @Syslog(module = "AUTH", value = "提交重置新密码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/password/reset")
    public ApiResponseEntity<RecoveryFlowResponseVO> submitPasswordReset(
            @Valid @RequestBody PasswordRecoveryResetRequestDTO request) {
        RecoveryFlowResponseVO response = recoveryService.submitPasswordReset(request);
        return ApiResponseEntity.ok("密码已成功重置", response);
    }

}
