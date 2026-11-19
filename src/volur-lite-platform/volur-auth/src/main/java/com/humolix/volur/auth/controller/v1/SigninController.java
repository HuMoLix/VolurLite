package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.signin.*;
import com.humolix.volur.auth.domain.vo.AccountChooserResponseVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.auth.service.SignInService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.ratelimit.annotation.RateLimit;
import com.humolix.volur.framework.ratelimit.enums.RateLimitType;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/signin")
public class SignInController {

    private final SignInService signinService;

    @GetMapping("/accountchooser")
    public ApiResponseEntity<AccountChooserResponseVO> getAccounts(
            @Valid AccountChooserProbeRequestDTO request) {
        AccountChooserResponseVO response = signinService.getAccounts(request);
        return ApiResponseEntity.ok("获取账号列表成功", response).cacheControl(CacheControl.noStore());
    }

    @Syslog(module = "AUTH", value = "选择账号推进认证流", logType = "AUTH", recordParams = false)
    @PostMapping("/accountchooser/select")
    public ApiResponseEntity<SignInFlowResponseVO> selectAccount(
            @Valid @RequestBody AccountChooserSelectRequestDTO request) {
        SignInFlowResponseVO response = signinService.selectAccount(request);
        return ApiResponseEntity.ok("账号选择成功", response);
    }

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
    @RateLimit(key = "#request.dsh", type = RateLimitType.BRUTE_FORCE, count = 5, window = 900, lockSeconds = 900, message = "连续认证失败次数过多，账号已临时锁定")
    @PostMapping("/authenticate")
    public ApiResponseEntity<SignInFlowResponseVO> authenticate(
            @Valid @RequestBody SignInAuthenticateRequestDTO request) {
        SignInFlowResponseVO response = signinService.authenticate(request);
        return ApiResponseEntity.ok("用户登录成功", response);
    }

}
