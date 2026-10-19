package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.TOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.TotpEnableRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.TotpEnableResponseVO;
import com.humolix.volur.security.domain.vo.TotpSetupVO;
import com.humolix.volur.security.domain.vo.TotpStatusVO;
import com.humolix.volur.security.service.TotpService;
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
@RequestMapping("/api/v1/captcha/challenge/totp")
public class TOTPChallengeController {

    private final TotpService totpService;

    @Syslog(module = "SECURITY", value = "获取动态身份验证器配置", logType = "AUTH", recordParams = false)
    @GetMapping("/setup")
    public ApiResponseEntity<TotpSetupVO> setupTotp(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        TotpSetupVO response = totpService.setupTotp(dsh, tl);
        return ApiResponseEntity.ok("获取动态身份验证器配置成功", response);
    }

    @Syslog(module = "SECURITY", value = "核验并启用动态身份验证器", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/enable")
    public ApiResponseEntity<TotpEnableResponseVO> enableTotp(
            @Valid @RequestBody TotpEnableRequestDTO request) {
        TotpEnableResponseVO response = totpService.enableTotp(request);
        return ApiResponseEntity.ok("动态身份验证器绑定成功", response);
    }

    @Syslog(module = "SECURITY", value = "核销动态身份验证器登录挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping(value = {"/verify", ""})
    public ApiResponseEntity<FlowResponseVO> verifyTotp(
            @Valid @RequestBody TOTPChallengeRequestDTO request) {
        FlowResponseVO response = totpService.verifyTotp(request);
        return ApiResponseEntity.ok("安全口令核验通过", response);
    }

    @Syslog(module = "SECURITY", value = "获取动态身份验证器状态", logType = "OPERATION")
    @GetMapping("/status")
    public ApiResponseEntity<TotpStatusVO> getStatus(@CurrentUserId Long userId) {
        TotpStatusVO response = totpService.getTotpStatus(userId);
        return ApiResponseEntity.ok("获取动态身份验证器状态成功", response);
    }

    @Syslog(module = "SECURITY", value = "解绑动态身份验证器", logType = "OPERATION")
    @PostMapping("/disable")
    public ApiResponseEntity<Void> disable(@CurrentUserId Long userId) {
        totpService.disableTotp(userId);
        return ApiResponseEntity.success("动态身份验证器解绑成功");
    }

}
