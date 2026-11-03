package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum TeamStatus implements EnumEntity {

    AUDIT_DRAFT(0, "AUDIT_DRAFT"),
    AUDIT_PASSED(1, "AUDIT_PASSED"),
    AUDIT_REJECTED(2, "AUDIT_REJECTED"),
    BANNED(3, "BANNED"),
    ENDED(4, "ENDED");

    private final int code;
    @JsonValue
    private final String description;

    TeamStatus(int code, String description) {
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
