package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.EmailOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge/emailotp")
public class EmailOTPChallengeController {

    private final ChallengeService challengeService;

    @Syslog(module = "SECURITY", value = "核销邮箱安全验证码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping
    public ApiResponseEntity<FlowResponseVO> verify(
            @Valid @RequestBody EmailOTPChallengeRequestDTO request) {
        FlowResponseVO response = challengeService.verifyEmailOTP(request);
        return ApiResponseEntity.ok("邮箱安全验证通过", response);
    }

    @Syslog(module = "SECURITY", value = "重新发送邮箱安全验证码", logType = "AUTH", recordParams = false)
    @PostMapping("/send")
    public ApiResponseEntity<Void> send(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        challengeService.sendEmailOTP(dsh, tl);
        return ApiResponseEntity.success("邮箱验证码已重新发送");
    }

}
