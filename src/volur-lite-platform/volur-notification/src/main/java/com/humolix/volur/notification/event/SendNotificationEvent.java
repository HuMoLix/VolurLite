package com.humolix.volur.notification.event;

import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Getter
public class SendNotificationEvent extends ApplicationEvent {

    private final Long senderId;
    private final Long receiverId;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final NotificationActionStatus actionStatus;
    private final String actionData;
    private final Instant expireTime;

    @Builder
    public SendNotificationEvent(
            Object source,
            Long senderId,
            Long receiverId,
            NotificationType type,
            String title,
            String content,
            NotificationActionStatus actionStatus,
            String actionData,
            Instant expireTime
    ) {
        super(source != null ? source : "NotificationEventPublisher");
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type != null ? type : NotificationType.SYSTEM_NOTICE;
        this.title = title;
        this.content = content;
        this.actionStatus = actionStatus != null ? actionStatus : NotificationActionStatus.NONE;
        this.actionData = actionData;
        this.expireTime = expireTime;
    }

    public static SendNotificationEvent of(Object source, Long senderId, Long receiverId, NotificationType type, String content) {
        return SendNotificationEvent.builder()
                .source(source)
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .content(content)
                .build();
    }

    public static SendNotificationEvent of(Object source, Long senderId, Long receiverId, NotificationType type, String title, String content) {
        return SendNotificationEvent.builder()
                .source(source)
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .title(title)
                .content(content)
                .build();
    }

    public static SendNotificationEvent system(Object source, Long receiverId, NotificationType type, String title, String content) {
        return SendNotificationEvent.builder()
                .source(source)
                .senderId(null)
                .receiverId(receiverId)
                .type(type)
                .title(title)
                .content(content)
                .build();
    }

    public SendNotificationBO toBO() {
        String effectiveTitle = StringUtils.hasText(title)
                ? title
                : "【" + (type != null ? type.getDescription() : "系统通知") + "】";

        return SendNotificationBO.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .title(effectiveTitle)
                .content(content)
                .actionStatus(actionStatus != null ? actionStatus : NotificationActionStatus.NONE)
                .actionData(actionData)
                .expireTime(expireTime)
                .build();
    }

}
