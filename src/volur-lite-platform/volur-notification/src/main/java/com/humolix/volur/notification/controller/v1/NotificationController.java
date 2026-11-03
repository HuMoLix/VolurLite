package com.humolix.volur.notification.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.notification.domain.dto.NotificationQueryDTO;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import com.humolix.volur.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    public ApiResponseEntity<Long> getUnreadCount(@CurrentUserId Long userId) {
        return ApiResponseEntity.ok("获取未读通知数成功", notificationService.getUnreadCount(userId));
    }

    @GetMapping
    public ApiResponseEntity<PageResponse<NotificationVO>> listNotifications(
            @CurrentUserId Long userId,
            NotificationQueryDTO queryDTO) {
        return ApiResponseEntity.ok("获取通知列表成功", notificationService.listNotifications(userId, queryDTO));
    }

    @GetMapping("/{id}")
    public ApiResponseEntity<NotificationVO> getNotificationDetail(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        return ApiResponseEntity.ok("获取通知详情成功", notificationService.getNotificationDetail(userId, id));
    }

    @PatchMapping("/read-all")
    public ApiResponseEntity<Void> markAllAsRead(@CurrentUserId Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponseEntity.success("已全部标记为已读");
    }

    @DeleteMapping("/{id}")
    public ApiResponseEntity<Void> deleteNotification(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        notificationService.deleteNotification(userId, id);
        return ApiResponseEntity.success("通知删除成功");
    }

}
