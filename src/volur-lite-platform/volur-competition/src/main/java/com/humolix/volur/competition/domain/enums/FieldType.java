package com.humolix.volur.competition.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.common.entity.enums.EnumEntity;

public enum FieldType implements EnumEntity {

    TEXT(0, "TEXT"),
    NUMBER(1, "NUMBER"),
    SELECT(2, "SELECT"),
    ID_CARD(3, "ID_CARD"),
    PHONE(4, "PHONE"),
    IMAGE_UPLOAD(5, "IMAGE_UPLOAD");

    private final int code;
    @JsonValue
    private final String description;

    FieldType(int code, String description) {
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
