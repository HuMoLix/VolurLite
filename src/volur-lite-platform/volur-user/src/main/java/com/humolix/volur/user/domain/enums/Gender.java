package com.humolix.volur.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum Gender implements EnumEntity {
    UNKNOWN(0, "UNKNOWN"),
    MALE(1, "MALE"),
    FEMALE(2, "FEMALE");

    private final int code;
    @JsonValue
    private final String description;

    Gender(int code, String description) {
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
