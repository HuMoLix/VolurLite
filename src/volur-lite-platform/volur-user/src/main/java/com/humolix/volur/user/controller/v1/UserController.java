package com.humolix.volur.user.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.service.UserService;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        Object principal = authentication.getPrincipal();
        try {
            Method method = null;
            if (principal != null) {
                method = principal.getClass().getMethod("getUserId");
            }
            if (method != null) {
                return (Long) method.invoke(principal);
            }
        } catch (Exception e) {
            try {
                return Long.parseLong(authentication.getName());
            } catch (Exception ex) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "无法识别当前用户身份");
            }
        }
        return 0L;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummaryVO> getCurrentUser() {
        Long userId = getCurrentUserId();
        return ApiResponseEntity.ok("获取当前用户信息成功", userService.getCurrentUser(userId));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponseVO> getCurrentUserProfile() {
        Long userId = getCurrentUserId();
        return ApiResponseEntity.ok("获取个人资料成功", userService.getUserProfileById(userId));
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileResponseVO> getUserProfileById(@PathVariable Long userId) {
        return ApiResponseEntity.ok("获取用户资料成功", userService.getUserProfileById(userId));
    }

}
