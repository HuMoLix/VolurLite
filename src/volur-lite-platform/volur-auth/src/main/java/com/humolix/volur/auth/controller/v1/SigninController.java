package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录流转引擎", description = "基于 Google Accounts 规范的会话句柄 (dsh) 与流转锁 (TL) 多步骤登录鉴权 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/signin")
public class SigninController {

    private final SigninService signinService;

    @Operation(summary = "提交登录账号标识并评估安全挑战 (Step: Identifier)")
    @Syslog(module = "AUTH", value = "提交账号标识", logType = "AUTH", recordParams = false)
    @PostMapping("/identifier")
    public ApiResponse<SignInFlowResponseVO> identifier(
            @Valid @RequestBody SignInIdentifierRequestDTO request) {
        SignInFlowResponseVO response = signinService.identifier(request);
        return ApiResponseEntity.ok("账号核验成功", response);
    }

    @Operation(summary = "提交安全挑战验证并推进流转锁 (Step: Challenge)")
    @Syslog(module = "AUTH", value = "提交安全挑战验证", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/challenge")
    public ApiResponse<SignInFlowResponseVO> challenge(
            @Valid @RequestBody SignInChallengeRequestDTO request) {
        SignInFlowResponseVO response = signinService.challenge(request);
        return ApiResponseEntity.ok("挑战核验处理完成", response);
    }

}
