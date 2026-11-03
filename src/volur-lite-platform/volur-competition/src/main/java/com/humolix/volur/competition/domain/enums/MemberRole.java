package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum MemberRole implements EnumEntity {

    CAPTAIN(0, "CAPTAIN"),
    LEADER(1, "LEADER"),
    MEMBER(2, "MEMBER");

    private final int code;
    @JsonValue
    private final String description;

    MemberRole(int code, String description) {
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
