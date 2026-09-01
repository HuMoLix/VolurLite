package com.humolix.volur.notification.service;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.bo.UpdateNotificationActionBO;
import com.humolix.volur.notification.domain.dto.NotificationQueryDTO;
import com.humolix.volur.notification.domain.vo.NotificationVO;

public interface NotificationService {

    long getUnreadCount(Long userId);

    PageResponse<NotificationVO> listNotifications(Long userId, NotificationQueryDTO queryDTO);

    NotificationVO getNotificationDetail(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    void sendNotification(SendNotificationBO sendBO);

    void updateActionStatus(UpdateNotificationActionBO updateBO);

}
