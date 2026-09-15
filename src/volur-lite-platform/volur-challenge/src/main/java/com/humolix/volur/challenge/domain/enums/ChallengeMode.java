package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum ChallengeMode implements EnumEntity {

    CTF(1, "CTF"),
    THEORY(2, "THEORY"),
    AWD(3, "AWD"),
    AWD_PLUS(4, "AWD_PLUS"),
    ISW(5, "ISW");

    private final int code;
    @JsonValue
    private final String description;

    ChallengeMode(int code, String description) {
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
