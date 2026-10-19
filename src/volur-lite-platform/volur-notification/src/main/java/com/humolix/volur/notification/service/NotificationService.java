package com.humolix.volur.notification.service;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.bo.UpdateNotificationActionBO;
import com.humolix.volur.notification.domain.dto.NotificationQueryDTO;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.domain.vo.NotificationVO;

import java.time.Instant;

public interface NotificationService {

    long getUnreadCount(Long userId);

    PageResponse<NotificationVO> listNotifications(Long userId, NotificationQueryDTO queryDTO);

    NotificationVO getNotificationDetail(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    void sendNotification(SendNotificationBO sendBO);

    void sendNotification(Long senderId, Long receiverId, NotificationType type, String content);

    void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content);

    void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, String actionData, Instant expireTime);

    void sendNotification(Long senderId, Long receiverId, NotificationType type, String title, String content, NotificationActionStatus actionStatus, String actionData, Instant expireTime);

    void updateActionStatus(UpdateNotificationActionBO updateBO);

    void updateActionStatus(Long receiverId, NotificationType type, String actionDataPattern, NotificationActionStatus newActionStatus);

}
