package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.ReCAPTCHAChallengeSubmitDTO;
import com.humolix.volur.security.domain.dto.RecaptchaVerifyRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.RecaptchaChallengeVO;
import com.humolix.volur.security.domain.vo.RecaptchaVerifyResponseVO;
import com.humolix.volur.security.service.ChallengeService;
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
public class ReCAPTCHAChallengeController {

    private final RecaptchaService recaptchaService;
    private final ChallengeService challengeService;

    @Syslog(module = "SECURITY", value = "获取 reCAPTCHA 挑战", logType = "AUTH", recordParams = false)
    @GetMapping
    public ApiResponseEntity<RecaptchaChallengeVO> getRecaptchaChallenge(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        RecaptchaChallengeVO response = recaptchaService.getRecaptchaChallenge(dsh, tl);
        return ApiResponseEntity.ok("获取人机验证挑战成功", response);
    }

    @Syslog(module = "SECURITY", value = "核验 reCAPTCHA 挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/verify")
    public ApiResponseEntity<RecaptchaVerifyResponseVO> verifyRecaptcha(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @Valid @RequestBody RecaptchaVerifyRequestDTO request) {
        RecaptchaVerifyResponseVO response = recaptchaService.verifyRecaptcha(dsh, tl, request);
        return ApiResponseEntity.ok("人机身份验证通过", response);
    }

    @Syslog(module = "SECURITY", value = "提交并推进 reCAPTCHA 挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping
    public ApiResponseEntity<FlowResponseVO> submitRecaptcha(
            @Valid @RequestBody ReCAPTCHAChallengeSubmitDTO request) {
        FlowResponseVO response = challengeService.verifyReCAPTCHA(request);
        return ApiResponseEntity.ok("人机验证流转处理完成", response);
    }

}
