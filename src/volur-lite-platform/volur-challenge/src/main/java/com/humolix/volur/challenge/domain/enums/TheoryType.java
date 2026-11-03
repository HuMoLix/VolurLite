package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum TheoryType implements EnumEntity {

    SINGLE_CHOICE(0, "SINGLE_CHOICE"),
    MULTIPLE_CHOICE(1, "MULTIPLE_CHOICE"),
    TRUE_FALSE(2, "TRUE_FALSE"),
    ESSAY(3, "ESSAY");

    private final int code;
    @JsonValue
    private final String description;

    TheoryType(int code, String description) {
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
