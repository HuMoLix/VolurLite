package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum FillScope implements EnumEntity {

    ALL_MEMBERS(0, "ALL_MEMBERS"),
    CAPTAIN_ONLY(1, "CAPTAIN_ONLY");

    private final int code;
    @JsonValue
    private final String description;

    FillScope(int code, String description) {
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
