package com.humolix.volur.notification.domain.bo;

import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationActionBO {

    private Long receiverId;

    private NotificationType type;

    private String actionDataPattern;

    private NotificationActionStatus newActionStatus;

}
