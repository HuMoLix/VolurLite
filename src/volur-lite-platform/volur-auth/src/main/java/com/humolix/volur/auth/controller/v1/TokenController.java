package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final TokenService tokenService;

    @Syslog(module = "AUTH", value = "用户登出", logType = "AUTH")
    @PostMapping("/logout")
    public ApiResponseEntity<Void> logout() {
        tokenService.logout();
        return ApiResponseEntity.success("登出成功");
    }

    @PostMapping("/refresh")
    public ApiResponseEntity<Void> refreshToken() {
        tokenService.refreshToken();
        return ApiResponseEntity.success("Token 刷新成功");
    }

}
