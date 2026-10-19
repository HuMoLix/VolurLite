package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum AwdCategory implements EnumEntity {

    WEB(0, "WEB"),
    PWN(1, "PWN"),
    MISC(2, "MISC");

    private final int code;
    @JsonValue
    private final String description;

    AwdCategory(int code, String description) {
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
