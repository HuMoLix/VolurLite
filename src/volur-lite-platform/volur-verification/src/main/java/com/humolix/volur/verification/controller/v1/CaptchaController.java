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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "验证码与人机核验", description = "图形验证码、行为式人机验证挑战生成与凭证核发接口")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final CaptchaSDK captchaSDK;
    private final CaptchaCryptoService captchaCryptoService;

    @Operation(summary = "获取自适应人机验证 RSA 公钥 (用于客户端场景加密)")
    @GetMapping("/public-key")
    public ApiResponse<Map<String, String>> getCaptchaPublicKey() {
        String publicKey = captchaCryptoService.getPublicKeyPem();
        return ApiResponseEntity.ok("获取公钥成功", Map.of("publicKey", publicKey));
    }

    @Operation(summary = "获取自适应人机验证挑战/验证码 (场景加密主入口)")
    @PostMapping
    public ApiResponse<CaptchaResponseVO> getCaptcha(@Valid @RequestBody CaptchaGenerateRequestDTO request) {
        CaptchaResponseVO captchaResponse = captchaSDK.generateCaptchaWithEncryptedScene(request);
        return ApiResponseEntity.ok("获取验证码成功", captchaResponse);
    }

    @Operation(summary = "获取场景化行为式人机验证挑战 (滑块拼图等)")
    @GetMapping("/challenge")
    public ApiResponse<CaptchaResponseVO> getCaptchaChallenge(
            @RequestParam(value = "scene", defaultValue = "DEFAULT") String scene,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateChallenge(scene, type, oldCaptchaId);
        return ApiResponseEntity.ok("获取人机验证挑战成功", captchaResponse);
    }

    @Operation(summary = "核验验证码并颁发通行凭证 (Ticket)")
    @PostMapping("/verify-ticket")
    public ApiResponse<CaptchaTicketVO> verifyAndIssueTicket(@Valid @RequestBody CaptchaVerifyDTO request) {
        CaptchaTicketVO ticketVO = captchaService.verifyAndIssueTicket(request);
        return ApiResponseEntity.ok("人机验证成功", ticketVO);
    }

    @Operation(summary = "极高风控场景下请求下发带外动态安全码 (OTP)")
    @PostMapping("/send-otp")
    public ApiResponse<Void> sendLoginOtp(@Valid @RequestBody CaptchaOtpRequestDTO request) {
        captchaSDK.sendLoginOtp(request.getAccount(), request.getCaptchaTicket());
        return ApiResponseEntity.success("动态安全码已发送至绑定邮箱");
    }

}
