package com.humolix.volur.verification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO;
import com.humolix.volur.verification.domain.vo.CaptchaResponseVO;
import com.humolix.volur.verification.domain.vo.CaptchaTicketVO;
import com.humolix.volur.verification.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "验证码与人机核验", description = "图形验证码、行为式人机验证挑战生成与凭证核发接口")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取场景化人机验证挑战")
    @GetMapping("/challenge")
    public ApiResponse<CaptchaResponseVO> getCaptchaChallenge(
            @RequestParam(value = "scene", defaultValue = "DEFAULT") String scene,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateChallenge(scene, type, oldCaptchaId);
        return ApiResponseEntity.ok("获取人机验证挑战成功", captchaResponse);
    }

    @Operation(summary = "获取通用图形验证码")
    @GetMapping
    public ApiResponse<CaptchaResponseVO> getCaptcha(
            @RequestParam(value = "oldCaptchaId", required = false) String oldCaptchaId,
            @RequestParam(value = "ticket", required = false) String ticket
    ) {
        CaptchaResponseVO captchaResponse = captchaService.generateCaptcha(oldCaptchaId, ticket);
        return ApiResponseEntity.ok("获取验证码成功", captchaResponse);
    }

    @Operation(summary = "核验验证码并颁发通行凭证 (Ticket)")
    @PostMapping("/verify-ticket")
    public ApiResponse<CaptchaTicketVO> verifyAndIssueTicket(@Valid @RequestBody CaptchaVerifyDTO request) {
        CaptchaTicketVO ticketVO = captchaService.verifyAndIssueTicket(request);
        return ApiResponseEntity.ok("人机验证成功", ticketVO);
    }

}
