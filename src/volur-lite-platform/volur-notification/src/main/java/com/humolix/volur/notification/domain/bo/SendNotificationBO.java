package com.humolix.volur.notification.domain.bo;

import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
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
public class SendNotificationBO {

    private Long senderId;

    private Long receiverId;

    private String title;

    private String content;

    private NotificationType type;

    @Builder.Default
    private NotificationActionStatus actionStatus = NotificationActionStatus.NONE;

    private String actionData;

    private Instant expireTime;

}
