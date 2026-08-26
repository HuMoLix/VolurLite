package com.humolix.volur.notification.domain.vo;

import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;

    private Long senderId;

    private String senderUsername;

    private Long receiverId;

    private String title;

    private String content;

    private NotificationType type;

    private NotificationStatus status;

    private NotificationActionStatus actionStatus;

    private String actionData;

    private Instant expireTime;

    private Instant readTime;

    private Instant createdAt;

}
