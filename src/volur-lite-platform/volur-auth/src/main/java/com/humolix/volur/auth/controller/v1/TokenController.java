package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.RefreshTokenRequestDTO;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "令牌与会话管理", description = "用户登出与 JWT 刷新令牌相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final AuthService authService;

    @Operation(summary = "用户登出")
    @Syslog(module = "AUTH", value = "用户登出", logType = "AUTH")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponseEntity.success("登出成功");
    }

    @Operation(summary = "刷新 JWT 访问令牌")
    @PostMapping("/refresh")
    public ApiResponse<AuthenticateResponseVO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthenticateResponseVO response = authService.refreshToken(request.getRefreshToken());
        return ApiResponseEntity.ok("Token 刷新成功", response);
    }

}
