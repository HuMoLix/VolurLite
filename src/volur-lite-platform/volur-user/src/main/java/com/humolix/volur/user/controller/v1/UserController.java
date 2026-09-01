package com.humolix.volur.user.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.user.domain.dto.UpdateAvatarRequestDTO;
import com.humolix.volur.user.domain.dto.UpdateUserProfileRequestDTO;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import com.humolix.volur.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户个人中心", description = "获取与更新当前用户资料、上传头像、修改密码与账号注销接口")
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

    @Operation(summary = "更新当前用户个人资料与隐私设置")
    @Syslog(module = "USER", value = "更新个人资料", logType = "OPERATION")
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponseVO> updateUserProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateUserProfileRequestDTO request) {
        UserProfileResponseVO response = userService.updateUserProfile(userId, request);
        return ApiResponseEntity.ok("个人资料更新成功", response);
    }

    @Operation(summary = "上传并更新当前用户头像文件")
    @Syslog(module = "USER", value = "上传头像文件", logType = "OPERATION", recordParams = false)
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @CurrentUserId Long userId,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAndSetAvatar(userId, file);
        return ApiResponseEntity.ok("头像上传成功", avatarUrl);
    }

    @Operation(summary = "直接设置当前用户头像URL")
    @Syslog(module = "USER", value = "更新头像URL", logType = "OPERATION")
    @PutMapping("/avatar")
    public ApiResponse<String> updateAvatar(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateAvatarRequestDTO request) {
        String avatarUrl = userService.updateAvatar(userId, request.getAvatarUrl());
        return ApiResponseEntity.ok("头像更新成功", avatarUrl);
    }

}
