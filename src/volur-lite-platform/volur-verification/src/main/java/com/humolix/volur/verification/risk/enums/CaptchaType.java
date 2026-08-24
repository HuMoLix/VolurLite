package com.humolix.volur.verification.risk.enums;

import org.springframework.util.StringUtils;

public enum CaptchaType {
    LINE,
    MATH,
    SHEAR,
    SLIDE_PUZZLE,
    CLOUD_TURNSTILE;

    public static CaptchaType fromString(String typeStr) {
        if (!StringUtils.hasText(typeStr)) {
            return null;
        }
        for (CaptchaType type : values()) {
            if (type.name().equalsIgnoreCase(typeStr.trim())) {
                return type;
            }
        }
        return null;
    }
}
