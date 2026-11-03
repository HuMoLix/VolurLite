package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum ChatMessageType implements EnumEntity {

    TEXT(0, "TEXT"),
    CODE(1, "CODE"),
    SYSTEM(2, "SYSTEM");

    private final int code;
    @JsonValue
    private final String description;

    ChatMessageType(int code, String description) {
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
