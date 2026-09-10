package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum CompetitionStage implements EnumEntity {

    THEORY(1, "THEORY"),
    CTF(2, "CTF"),
    AWD(3, "AWD"),
    AWD_PLUS(4, "AWD_PLUS"),
    ISW(5, "ISW");

    private final int code;
    @JsonValue
    private final String description;

    CompetitionStage(int code, String description) {
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
