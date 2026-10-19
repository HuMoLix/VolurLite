package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum SubmissionStatus implements EnumEntity {

    CORRECT(1, "CORRECT"),
    WRONG(2, "WRONG"),
    DUPLICATE(3, "DUPLICATE"),
    INVALID(4, "INVALID");

    private final int code;
    @JsonValue
    private final String description;

    SubmissionStatus(int code, String description) {
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
