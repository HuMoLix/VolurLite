package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum ChatChannelType implements EnumEntity {

    TEAM(0, "TEAM"),
    REFEREE(1, "REFEREE"),
    PRIVATE(2, "PRIVATE");

    private final int code;
    @JsonValue
    private final String description;

    ChatChannelType(int code, String description) {
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
