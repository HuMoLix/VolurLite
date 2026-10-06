package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.security.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge/pwd")
public class PasswordController {

    private final ChallengeService challengeService;

    @Syslog(module = "SECURITY", value = "核销密码挑战", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping
    public ApiResponseEntity<SignInFlowResponseVO> verify(
            @Valid @RequestBody PasswordChallengeRequestDTO request) {
        SignInFlowResponseVO response = challengeService.verifyPassword(request);
        return ApiResponseEntity.ok("密码安全核验通过", response);
    }

}
