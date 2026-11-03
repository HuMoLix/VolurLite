package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum AwdRoundStatus implements EnumEntity {

    PENDING(0, "PENDING"),
    RUNNING(1, "RUNNING"),
    ENDED(2, "ENDED");

    private final int code;
    @JsonValue
    private final String description;

    AwdRoundStatus(int code, String description) {
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
