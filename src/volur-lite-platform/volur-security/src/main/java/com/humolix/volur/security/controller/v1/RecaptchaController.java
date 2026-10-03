package com.humolix.volur.security.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.service.RecaptchaService;
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
@RequestMapping("/api/v1/captcha/challenge/recaptcha")
public class RecaptchaController {

    private final RecaptchaService recaptchaService;

    @Syslog(module = "SECURITY", value = "获取 reCAPTCHA 挑战", logType = "AUTH", recordParams = false)
    @GetMapping
    public ApiResponseEntity<RecaptchaChallengeVO> getRecaptchaChallenge(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        RecaptchaChallengeVO response = recaptchaService.getRecaptchaChallenge(dsh, tl);
        return ApiResponseEntity.ok("获取人机验证挑战成功", response);
    }

    @Syslog(module = "SECURITY", value = "核销 reCAPTCHA 挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping
    public ApiResponseEntity<RecaptchaVerifyResponseVO> verifyRecaptcha(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @Valid @RequestBody RecaptchaVerifyRequestDTO request) {
        RecaptchaVerifyResponseVO response = recaptchaService.verifyRecaptcha(dsh, tl, request);
        return ApiResponseEntity.ok("人机身份验证通过", response);
    }

}
