package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.RefreshTokenRequestDTO;
import com.humolix.volur.auth.exception.AuthException;
import com.humolix.volur.auth.service.AuthService;
import com.humolix.volur.auth.domain.vo.AuthenticateResponseVO;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final AuthService authService;

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) throws AuthException {
        authService.logout(request);
        return ApiResponseEntity.success("登出成功");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticateResponseVO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthenticateResponseVO response = authService.refreshToken(request.getRefreshToken());
        return ApiResponseEntity.ok("Token 刷新成功", response);
    }

}
