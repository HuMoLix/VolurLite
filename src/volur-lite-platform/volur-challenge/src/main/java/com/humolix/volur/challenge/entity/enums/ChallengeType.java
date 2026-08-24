package com.humolix.volur.challenge.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum ChallengeType implements EnumEntity {

    CTF(1, "CTF"),
    AWD(2, "AWD"),
    THEORY(3, "THEORY"),
    AWD_PLUS(4, "AWD_PLUS"),
    ISW(5, "ISW");

    private final int code;
    private final String description;

    ChallengeType(int code, String description) {
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
