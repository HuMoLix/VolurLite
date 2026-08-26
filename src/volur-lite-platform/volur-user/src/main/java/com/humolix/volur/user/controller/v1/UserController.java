package com.humolix.volur.user.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import com.humolix.volur.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户个人中心", description = "获取当前登录用户基本信息与个人公开资料接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户概要信息")
    @GetMapping("/me")
    public ApiResponse<UserSummaryVO> getCurrentUser(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取当前用户信息成功", userService.getCurrentUser(userId));
    }

    @Operation(summary = "获取当前登录用户详细个人资料")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponseVO> getCurrentUserProfile(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取个人资料成功", userService.getUserProfileById(userId));
    }

    @Operation(summary = "根据用户ID获取指定用户资料")
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileResponseVO> getUserProfileById(@PathVariable Long userId) {
        return ApiResponseEntity.ok("获取用户资料成功", userService.getUserProfileById(userId));
    }

}
