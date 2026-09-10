package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum ChallengeType implements EnumEntity {

    STATIC(0, "STATIC"),
    DYNAMIC(1, "DYNAMIC"),
    CONTAINER(2, "CONTAINER");

    private final int code;
    @JsonValue
    private final String description;

    ChallengeType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
