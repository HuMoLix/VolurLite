package com.humolix.volur.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationStatus {

    UNREAD("未读"),
    READ("已读");

    private final String description;

}
