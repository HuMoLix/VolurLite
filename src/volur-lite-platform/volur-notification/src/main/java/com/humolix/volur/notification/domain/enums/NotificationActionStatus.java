package com.humolix.volur.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationActionStatus {

    NONE("无动作"),
    PENDING("待处理"),
    ACCEPTED("已同意"),
    REJECTED("已拒绝"),
    EXPIRED("已失效"),
    CANCELLED("已撤销");

    private final String description;

}
