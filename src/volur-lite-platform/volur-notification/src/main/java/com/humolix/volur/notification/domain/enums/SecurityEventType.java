package com.humolix.volur.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityEventType {

    PASSWORD_CHANGED("密码修改成功"),
    PASSWORD_RESET("密码重置成功"),
    ACCOUNT_LOCKED("账号已被锁定"),
    ACCOUNT_UNLOCKED("账号已被解锁"),
    ACCOUNT_DISABLED("账号已被封禁"),
    ACCOUNT_DELETED("账号已被注销"),
    ROLE_CHANGED("角色权限变动");

    private final String description;

}
