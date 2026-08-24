package com.humolix.volur.challenge.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum SubmissionStatus implements EnumEntity {

    PROCESSING(1, "PROCESSING"),
    SUCCESS(2, "SUCCESS"),
    FAILED(3, "FAILED"),
    PENDING(4, "PENDING");

    private final int code;
    private final String description;

    SubmissionStatus(int code, String description) {
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
