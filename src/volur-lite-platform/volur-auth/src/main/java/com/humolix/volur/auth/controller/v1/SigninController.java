package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.SignInChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.ChallengeOptionVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;
import com.humolix.volur.auth.service.SigninService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/signin")
public class SigninController {

    private final SigninService signinService;

    @Syslog(module = "AUTH", value = "提交账号标识", logType = "AUTH", recordParams = false)
    @PostMapping("/identifier")
    public ApiResponseEntity<SignInFlowResponseVO> identifier(
            @Valid @RequestBody SignInIdentifierRequestDTO request) {
        SignInFlowResponseVO response = signinService.identifier(request);
        return ApiResponseEntity.ok("账号核验成功", response);
    }

    @Syslog(module = "AUTH", value = "提交安全挑战验证", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/challenge")
    public ApiResponseEntity<SignInFlowResponseVO> challenge(
            @Valid @RequestBody SignInChallengeRequestDTO request) {
        SignInFlowResponseVO response = signinService.challenge(request);
        return ApiResponseEntity.ok("挑战核验处理完成", response);
    }

    @Syslog(module = "AUTH", value = "获取备选核验方式", logType = "AUTH", recordParams = false)
    @GetMapping("/challenge/selection")
    public ApiResponseEntity<List<ChallengeOptionVO>> getChallengeSelection(
            @RequestParam(value = "dsh", required = false) String dsh,
            @RequestParam(value = "currentType", required = false) String currentType) {
        List<ChallengeOptionVO> response = signinService.getChallengeSelection(dsh, currentType);
        return ApiResponseEntity.ok("获取可选安全挑战方式成功", response);
    }

}
