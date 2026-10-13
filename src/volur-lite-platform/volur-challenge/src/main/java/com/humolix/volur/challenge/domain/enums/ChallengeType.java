package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

public enum ChallengeType implements EnumEntity {

    STATIC(0, "STATIC"),
    DYNAMIC(1, "DYNAMIC"),
    CONTAINER(2, "CONTAINER");

    private final int code;
    @JsonValue
    private final String description;

    ChallengeType(int code, String description) {
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

    @JsonCreator
    public static ChallengeType fromValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number num) {
            return fromCode(num.intValue());
        }
        String str = value.toString().trim().toUpperCase();
        for (ChallengeType type : values()) {
            if (type.name().equalsIgnoreCase(str) || type.description.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }

    public static ChallengeType fromCode(int code) {
        for (ChallengeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

}
