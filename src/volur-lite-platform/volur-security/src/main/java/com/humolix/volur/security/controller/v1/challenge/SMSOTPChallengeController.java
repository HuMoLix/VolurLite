package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.SMSOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge/smsotp")
public class SMSOTPChallengeController {

    private final ChallengeService challengeService;

    @Syslog(module = "SECURITY", value = "核销安全动态口令", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping
    public ApiResponseEntity<FlowResponseVO> verify(
            @Valid @RequestBody SMSOTPChallengeRequestDTO request) {
        FlowResponseVO response = challengeService.verifySMSOTP(request);
        return ApiResponseEntity.ok("安全口令验证通过", response);
    }

}
