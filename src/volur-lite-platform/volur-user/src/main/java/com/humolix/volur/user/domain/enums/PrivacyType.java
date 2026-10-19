package com.humolix.volur.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum PrivacyType implements EnumEntity {
    PUBLIC(0, "PUBLIC"),
    PRIVATE(1, "PRIVATE");

    private final int code;
    @JsonValue
    private final String description;

    PrivacyType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return code;
    }

    public int getValue() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
