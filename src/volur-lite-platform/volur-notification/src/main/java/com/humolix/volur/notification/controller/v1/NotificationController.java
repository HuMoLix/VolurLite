package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.notification.domain.dto.NotificationQueryDTO;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import com.humolix.volur.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "站内信与通知管理", description = "用户站内信、未读消息统计、标记已读与通知删除接口")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取当前用户未读通知数")
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取未读通知数成功", notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "分页查询当前用户通知列表")
    @GetMapping
    public ApiResponse<PageResponse<NotificationVO>> listNotifications(
            @CurrentUserId Long userId,
            NotificationQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取通知列表成功", notificationService.listNotifications(userId, queryDTO));
    }

    @Operation(summary = "获取指定通知详情并自动标为已读")
    @GetMapping("/{id}")
    public ApiResponse<NotificationVO> getNotificationDetail(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        return ApiResponseEntity.ok("获取通知详情成功", notificationService.getNotificationDetail(userId, id));
    }

    @Operation(summary = "单个标记通知为已读")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return ApiResponseEntity.success("已标记为已读");
    }

    @Operation(summary = "一键全部标记为已读")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@CurrentUserId Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponseEntity.success("已全部标记为已读");
    }

    @Operation(summary = "删除指定通知")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        notificationService.deleteNotification(userId, id);
        return ApiResponseEntity.success("通知删除成功");
    }

}
