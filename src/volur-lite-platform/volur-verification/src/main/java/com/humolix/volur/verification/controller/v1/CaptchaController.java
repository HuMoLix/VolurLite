package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/challenge")
    public ApiResponse<CaptchaResponseVO> getCaptchaChallenge(
            @RequestParam(value = "scene", defaultValue = "DEFAULT") String scene,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateChallenge(scene, type, oldCaptchaId);
        return ApiResponseEntity.ok("获取人机验证挑战成功", captchaResponse);
    }

    @GetMapping
    public ApiResponse<CaptchaResponseVO> getCaptcha(
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateCaptcha(oldCaptchaId);
        return ApiResponseEntity.ok("获取验证码成功", captchaResponse);
    }

    @PostMapping("/verify-ticket")
    public ApiResponse<CaptchaTicketVO> verifyAndIssueTicket(@Valid @RequestBody CaptchaVerifyDTO request) {
        CaptchaTicketVO ticketVO = captchaService.verifyAndIssueTicket(request);
        return ApiResponseEntity.ok("人机验证成功", ticketVO);
    }

}
