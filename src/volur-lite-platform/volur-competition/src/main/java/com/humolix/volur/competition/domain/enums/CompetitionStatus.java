package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum CompetitionStatus implements EnumEntity {

    DRAFT(0, "DRAFT"),
    REGISTRATION(1, "REGISTRATION"),
    RUNNING(2, "RUNNING"),
    FROZEN(3, "FROZEN"),
    ENDED(4, "ENDED"),
    ARCHIVED(5, "ARCHIVED");

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
