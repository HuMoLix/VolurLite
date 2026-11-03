package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.crypto.CaptchaCryptoService;
import com.humolix.volur.verification.domain.dto.CaptchaGenerateRequestDTO;
import com.humolix.volur.verification.domain.dto.CaptchaOtpRequestDTO;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.sdk.CaptchaSDK;
import com.humolix.volur.verification.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final CaptchaSDK captchaSDK;
    private final CaptchaCryptoService captchaCryptoService;

    @GetMapping("/public-key")
    public ApiResponseEntity<Map<String, String>> getCaptchaPublicKey() {
        String publicKey = captchaCryptoService.getPublicKeyPem();
        return ApiResponseEntity.ok("获取公钥成功", Map.of("publicKey", publicKey));
    }

    @PostMapping
    public ApiResponseEntity<CaptchaResponseVO> getCaptcha(@Valid @RequestBody CaptchaGenerateRequestDTO request) {
        CaptchaResponseVO captchaResponse = captchaSDK.generateCaptchaWithEncryptedScene(request);
        return ApiResponseEntity.ok("获取验证码成功", captchaResponse);
    }

    @GetMapping("/challenge")
    public ApiResponseEntity<CaptchaResponseVO> getCaptchaChallenge(
            @RequestParam(value = "scene", defaultValue = "DEFAULT") String scene,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateChallenge(scene, type, oldCaptchaId);
        return ApiResponseEntity.ok("获取人机验证挑战成功", captchaResponse);
    }

    @PostMapping("/verify-ticket")
    public ApiResponseEntity<CaptchaTicketVO> verifyAndIssueTicket(@Valid @RequestBody CaptchaVerifyDTO request) {
        CaptchaTicketVO ticketVO = captchaService.verifyAndIssueTicket(request);
        return ApiResponseEntity.ok("人机验证成功", ticketVO);
    }

    @PostMapping("/send-otp")
    public ApiResponseEntity<Void> sendLoginOtp(@Valid @RequestBody CaptchaOtpRequestDTO request) {
        captchaSDK.sendLoginOtp(request.getAccount(), request.getCaptchaTicket());
        return ApiResponseEntity.success("动态安全码已发送至绑定邮箱");
    }

}
