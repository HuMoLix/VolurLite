package com.humolix.volur.user.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    SUPER_ADMIN("超级管理员"),
    ADMIN("管理员"),
    USER("普通用户");

    private final String description;

}
