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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponseEntity<UserSummaryVO> getCurrentUser(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取当前用户信息成功", userService.getCurrentUser(userId));
    }

    @GetMapping("/profile")
    public ApiResponseEntity<UserProfileResponseVO> getCurrentUserProfile(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取个人资料成功", userService.getUserProfileById(userId));
    }

    @GetMapping("/{userId}/profile")
    public ApiResponseEntity<UserProfileResponseVO> getUserProfileById(@PathVariable Long userId) {
        return ApiResponseEntity.ok("获取用户资料成功", userService.getUserProfileById(userId));
    }

    @Syslog(module = "USER", value = "更新个人资料", logType = "OPERATION")
    @PutMapping("/profile")
    public ApiResponseEntity<UserProfileResponseVO> updateUserProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateUserProfileRequestDTO request) {
        UserProfileResponseVO response = userService.updateUserProfile(userId, request);
        return ApiResponseEntity.ok("个人资料更新成功", response);
    }

    @Syslog(module = "USER", value = "上传头像文件", logType = "OPERATION", recordParams = false)
    @PostMapping("/avatar")
    public ApiResponseEntity<String> uploadAvatar(
            @CurrentUserId Long userId,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAndSetAvatar(userId, file);
        return ApiResponseEntity.ok("头像上传成功", avatarUrl);
    }

    @Syslog(module = "USER", value = "更新头像URL", logType = "OPERATION")
    @PutMapping("/avatar")
    public ApiResponseEntity<String> updateAvatar(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateAvatarRequestDTO request) {
        String avatarUrl = userService.updateAvatar(userId, request.getAvatarUrl());
        return ApiResponseEntity.ok("头像更新成功", avatarUrl);
    }

}
