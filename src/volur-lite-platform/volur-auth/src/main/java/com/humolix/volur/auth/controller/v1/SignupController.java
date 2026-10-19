package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.signup.SignUpCompleteRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEmailResendRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpEtotpChallengeRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpNameRequestDTO;
import com.humolix.volur.auth.domain.dto.signup.SignUpPasswordRequestDTO;
import com.humolix.volur.auth.domain.vo.SignUpFlowResponseVO;
import com.humolix.volur.auth.domain.vo.SignUpOptionVO;
import com.humolix.volur.auth.service.SignupService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/signup")
public class SignupController {

    private final SignupService signupService;

    @Syslog(module = "AUTH", value = "确认注册用户名", logType = "AUTH", recordParams = false)
    @PostMapping("/name")
    public ApiResponseEntity<SignUpFlowResponseVO> name(@Valid @RequestBody SignUpNameRequestDTO request) {
        SignUpFlowResponseVO response = signupService.name(request);
        return ApiResponseEntity.ok("用户名确认成功", response);
    }

    @Syslog(module = "AUTH", value = "提交注册邮箱", logType = "AUTH", recordParams = false)
    @PostMapping("/email")
    public ApiResponseEntity<SignUpFlowResponseVO> email(@Valid @RequestBody SignUpEmailRequestDTO request) {
        SignUpFlowResponseVO response = signupService.email(request);
        return ApiResponseEntity.ok("邮箱核验成功，验证码已发送", response);
    }

    @Syslog(module = "AUTH", value = "重发注册邮箱验证码", logType = "AUTH", recordParams = false)
    @PostMapping("/email/resend")
    public ApiResponseEntity<Void> resendEmailCode(@Valid @RequestBody SignUpEmailResendRequestDTO request) {
        signupService.resendEmailCode(request);
        return ApiResponseEntity.success("验证码已重新发送");
    }

    @Syslog(module = "AUTH", value = "核验注册邮箱验证码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/challenge/etotp")
    public ApiResponseEntity<SignUpFlowResponseVO> verifyEtotp(@Valid @RequestBody SignUpEtotpChallengeRequestDTO request) {
        SignUpFlowResponseVO response = signupService.verifyEtotp(request);
        return ApiResponseEntity.ok("邮箱安全核验通过", response);
    }

    @Syslog(module = "AUTH", value = "提交注册密码", logType = "AUTH", recordParams = false, recordResponse = false)
    @PostMapping("/password")
    public ApiResponseEntity<SignUpFlowResponseVO> password(@Valid @RequestBody SignUpPasswordRequestDTO request) {
        SignUpFlowResponseVO response = signupService.password(request);
        return ApiResponseEntity.ok("密码设置成功，用户已创建", response);
    }

    @Syslog(module = "AUTH", value = "获取注册可选安全扩展", logType = "AUTH", recordParams = false)
    @GetMapping("/protect/options")
    public ApiResponseEntity<List<SignUpOptionVO>> getOptions(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        List<SignUpOptionVO> response = signupService.getOptions(dsh, tl);
        return ApiResponseEntity.ok("获取可选安全选项成功", response);
    }



    @Syslog(module = "AUTH", value = "完成注册流程", logType = "AUTH", recordParams = false)
    @PostMapping("/complete")
    public ApiResponseEntity<SignUpFlowResponseVO> complete(@Valid @RequestBody SignUpCompleteRequestDTO request) {
        SignUpFlowResponseVO response = signupService.complete(request);
        return ApiResponseEntity.ok("注册流程完成", response);
    }

}
