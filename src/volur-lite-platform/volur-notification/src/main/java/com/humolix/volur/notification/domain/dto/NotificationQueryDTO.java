package com.humolix.volur.notification.domain.dto;

import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationQueryDTO {

    private Integer page;

    private Integer size;

    private String keyword;

    private NotificationType type;

    private NotificationStatus status;

    private NotificationActionStatus actionStatus;

}
