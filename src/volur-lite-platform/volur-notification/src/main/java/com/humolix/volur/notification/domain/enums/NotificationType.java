package com.humolix.volur.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // 通知型
    SECURITY_ALERT("安全告警"),
    ACCOUNT_CHANGE("账户变动"),
    COMPETITION_AUDIT("战队审核"),
    SYSTEM_NOTICE("系统通知"),

    // 交互型
    COMPETITION_INVITE("竞赛组队邀请"),
    COMPETITION_APPLICATION("入队申请");

    private final String description;

}
