package com.humolix.volur.security.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.service.PasskeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge/passkey")
public class PasskeyController {

    private final PasskeyService passkeyService;

    @Syslog(module = "SECURITY", value = "获取 Passkey 注册选项", logType = "AUTH", recordParams = false)
    @GetMapping
    public ApiResponseEntity<PublicKeyCredentialCreationOptions> getPasskeyCreationOptions(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @RequestParam(value = "authenticatorType", required = false) String authenticatorType) {
        PublicKeyCredentialCreationOptions response = passkeyService.getCreationOptions(dsh, tl, authenticatorType);
        return ApiResponseEntity.ok("获取通行密钥创建选项成功", response);
    }

    @Syslog(module = "SECURITY", value = "注册绑定 Passkey", logType = "AUTH", recordParams = false)
    @PostMapping
    public ApiResponseEntity<PasskeyRegisterResponseVO> registerPasskey(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @Valid @RequestBody PasskeyRegisterRequestDTO request) {
        PasskeyRegisterResponseVO response = passkeyService.registerPasskey(dsh, tl, request);
        return ApiResponseEntity.ok("通行密钥添加成功", response);
    }

    @Syslog(module = "SECURITY", value = "获取支持的 Passkey 认证器列表", logType = "AUTH", recordParams = false)
    @GetMapping("/authenticators")
    public ApiResponseEntity<List<PasskeyAuthenticatorOptionVO>> getSupportedAuthenticators() {
        List<PasskeyAuthenticatorOptionVO> response = passkeyService.getSupportedAuthenticators();
        return ApiResponseEntity.ok("获取支持的认证器列表成功", response);
    }

}
