package com.humolix.volur.notification.event;

import com.humolix.volur.notification.domain.enums.SecurityEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class SecurityNotificationEvent extends ApplicationEvent {

    private final Long userId;
    private final SecurityEventType eventType;
    private final String clientIp;
    private final String description;
    private final Instant occurTime;

    public SecurityNotificationEvent(Object source, Long userId, SecurityEventType eventType, String clientIp, String description) {
        super(source);
        this.userId = userId;
        this.eventType = eventType;
        this.clientIp = clientIp;
        this.description = description;
        this.occurTime = Instant.now();
    }

}
