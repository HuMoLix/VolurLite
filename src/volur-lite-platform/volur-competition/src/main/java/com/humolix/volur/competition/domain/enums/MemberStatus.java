package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum MemberStatus implements EnumEntity {

    INVITED(0, "INVITED"),
    INVITED_ACCEPTED(1, "INVITED_ACCEPTED"),
    NOT_PARTICIPATE(2, "NOT_PARTICIPATE");

    private final int code;
    @JsonValue
    private final String description;

    MemberStatus(int code, String description) {
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
