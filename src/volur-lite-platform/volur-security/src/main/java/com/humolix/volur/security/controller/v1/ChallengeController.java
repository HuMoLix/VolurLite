package com.humolix.volur.security.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "安全挑战引擎", description = "基于 VLIF 规范的动态人机验证码挑战下发与非对称加密核销 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "获取自适应 reCAPTCHA 人机安全挑战")
    @Syslog(module = "SECURITY", value = "获取 reCAPTCHA 挑战", logType = "AUTH", recordParams = false)
    @GetMapping("/recaptcha")
    public ApiResponse<RecaptchaChallengeVO> getRecaptchaChallenge(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        RecaptchaChallengeVO response = challengeService.getRecaptchaChallenge(dsh, tl);
        return ApiResponseEntity.ok("获取人机验证挑战成功", response);
    }

    @Operation(summary = "提交非对称加密 reCAPTCHA 验证载荷并核销签发通行票据")
    @Syslog(module = "SECURITY", value = "核销 reCAPTCHA 挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/recaptcha")
    public ApiResponse<RecaptchaVerifyResponseVO> verifyRecaptcha(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @Valid @RequestBody RecaptchaVerifyRequestDTO request) {
        RecaptchaVerifyResponseVO response = challengeService.verifyRecaptcha(dsh, tl, request);
        return ApiResponseEntity.ok("人机身份验证通过", response);
    }

}
