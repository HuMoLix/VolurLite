package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.signin.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserRemoveRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserSelectRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInAuthenticateRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/signin")
public class SigninController {

    private final SigninService signinService;

    @Syslog(module = "AUTH", value = "探测账号选择器状态", logType = "AUTH", recordParams = false)
    @PostMapping("/accountchooser/probe")
    public ApiResponseEntity<AccountChooserProbeResponseVO> probeAccounts(
            @Valid @RequestBody AccountChooserProbeRequestDTO request) {
        AccountChooserProbeResponseVO response = signinService.probeAccounts(request);
        return ApiResponseEntity.ok("账号状态探测成功", response);
    }

    @Syslog(module = "AUTH", value = "选择账号推进认证流", logType = "AUTH", recordParams = false)
    @PostMapping("/accountchooser/select")
    public ApiResponseEntity<SignInFlowResponseVO> selectAccount(
            @Valid @RequestBody AccountChooserSelectRequestDTO request) {
        SignInFlowResponseVO response = signinService.selectAccount(request);
        return ApiResponseEntity.ok("账号选择成功", response);
    }

    @Syslog(module = "AUTH", value = "从选择器移除账号", logType = "AUTH", recordParams = false)
    @PostMapping("/accountchooser/remove")
    public ApiResponseEntity<Void> removeAccount(
            @Valid @RequestBody AccountChooserRemoveRequestDTO request) {
        signinService.removeAccount(request);
        return ApiResponseEntity.success("账号已成功移除");
    }

    @PostMapping("/identifier")
    public ApiResponseEntity<SignInFlowResponseVO> identifier(
            @Valid @RequestBody SignInIdentifierRequestDTO request) {
        SignInFlowResponseVO response = signinService.identifier(request);
        return ApiResponseEntity.okData(response);
    }

    @Syslog(module = "AUTH", value = "用户登录成功", logType = "AUTH", recordParams = false, recordResponse = true)
    @PostMapping("/authenticate")
    public ApiResponseEntity<SignInFlowResponseVO> authenticate(
            @Valid @RequestBody SignInAuthenticateRequestDTO request) {
        SignInFlowResponseVO response = signinService.authenticate(request);
        return ApiResponseEntity.ok("用户登录成功", response);
    }

}
