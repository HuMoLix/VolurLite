package com.humolix.volur.user.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserStatusChangedEvent extends ApplicationEvent {

    private final Long userId;
    private final UserStatusAction action;

    public UserStatusChangedEvent(Object source, Long userId, UserStatusAction action) {
        super(source);
        this.userId = userId;
        this.action = action;
    }

}
