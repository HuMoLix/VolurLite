package com.humolix.volur.notification.service;

import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.vo.NotificationVO;

public interface RealtimePushService {

    void pushNotification(Long userId, NotificationVO notificationVO);

    void publishAnnouncement(BroadcastMessageDTO broadcastDTO);

    void publishPromotion(BroadcastMessageDTO promotionDTO);

    void broadcastToTopic(String topic, String eventName, Object payload);

    void broadcastAll(String eventName, Object payload);

    void sendToUser(Long userId, String eventName, Object payload);

}
