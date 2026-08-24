package com.humolix.volur.challenge.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum SceneType implements EnumEntity {

    COMPETITION(1, "COMPETITION"),
    TRAINING(2, "TRAINING");

    private final int code;
    private final String description;

    SceneType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return code;
    }

    @JsonValue
    @Override
    public String getDescription() {
        return description;
    }
}
