package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.security.domain.dto.PasskeyAssertionVerifyRequestDTO;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyCredentialVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.service.PasskeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/captcha/challenge/pk")
public class PasskeyController {

    private final PasskeyService passkeyService;

    @Syslog(module = "SECURITY", value = "获取 Passkey 注册选项", logType = "AUTH", recordParams = false)
    @GetMapping(params = {"dsh", "tl"})
    public ApiResponseEntity<PublicKeyCredentialCreationOptions> getPasskeyCreationOptions(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl,
            @RequestParam(value = "authenticatorType", required = false) String authenticatorType) {
        PublicKeyCredentialCreationOptions response = passkeyService.getCreationOptions(dsh, tl, authenticatorType);
        return ApiResponseEntity.ok("获取通行密钥创建选项成功", response);
    }

    @Syslog(module = "SECURITY", value = "注册绑定 Passkey", logType = "AUTH", recordParams = false)
    @PostMapping(params = {"dsh", "tl"})
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

    @Syslog(module = "SECURITY", value = "获取 Passkey 登录断言选项", logType = "AUTH", recordParams = false)
    @GetMapping(value = {"/options", "/assertion-options"})
    public ApiResponseEntity<PublicKeyCredentialRequestOptions> getPasskeyAssertionOptions(
            @RequestParam("dsh") String dsh,
            @RequestParam("tl") String tl) {
        PublicKeyCredentialRequestOptions response = passkeyService.getAssertionOptions(dsh, tl);
        return ApiResponseEntity.ok("获取通行密钥认证选项成功", response);
    }

    @Syslog(module = "SECURITY", value = "验证 Passkey 登录断言", logType = "AUTH", recordParams = false)
    @PostMapping(value = {"", "/verify"})
    public ApiResponseEntity<FlowResponseVO> verifyPasskeyAssertion(
            @Valid @RequestBody PasskeyAssertionVerifyRequestDTO request) {
        FlowResponseVO response = passkeyService.verifyPasskeyAssertion(request);
        return ApiResponseEntity.ok("通行密钥核验成功", response);
    }

    @Syslog(module = "SECURITY", value = "获取通行密钥凭据列表", logType = "OPERATION")
    @GetMapping("/credentials")
    public ApiResponseEntity<List<PasskeyCredentialVO>> listCredentials(@CurrentUserId Long userId) {
        List<PasskeyCredentialVO> response = passkeyService.listCredentials(userId);
        return ApiResponseEntity.ok("获取通行密钥凭据列表成功", response);
    }

    @Syslog(module = "SECURITY", value = "解绑通行密钥凭据", logType = "OPERATION")
    @DeleteMapping(value = {"/credentials/{credentialId}", "/{credentialId}"})
    public ApiResponseEntity<Void> deleteCredential(
            @CurrentUserId Long userId,
            @PathVariable("credentialId") String credentialId) {
        passkeyService.deleteCredential(userId, credentialId);
        return ApiResponseEntity.success("通行密钥凭据解绑成功");
    }

}
