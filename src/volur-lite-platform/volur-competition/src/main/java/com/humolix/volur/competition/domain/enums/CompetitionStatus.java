package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum CompetitionStatus implements EnumEntity {

    DRAFT(0, "DRAFT"),
    PUBLISHED(1, "PUBLISHED"),
    RUNNING(2, "RUNNING"),
    ENDED(3, "ENDED"),
    ARCHIVED(4, "ARCHIVED"),
    REGISTRATION(5, "REGISTRATION"),
    FROZEN(6, "FROZEN"),
    HIDDEN(7, "HIDDEN");

    private final int code;
    @JsonValue
    private final String description;

    CompetitionStatus(int code, String description) {
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
