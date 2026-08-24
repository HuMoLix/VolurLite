package com.humolix.volur.verification.risk.enums;

import org.springframework.util.StringUtils;

public enum CaptchaScene {
    LOGIN,
    REGISTER,
    CHANGE_PASSWORD,
    RESET_PASSWORD,
    SENSITIVE_OP,
    DEFAULT;

    public static CaptchaScene fromString(String sceneStr) {
        if (!StringUtils.hasText(sceneStr)) {
            return DEFAULT;
        }
        for (CaptchaScene scene : values()) {
            if (scene.name().equalsIgnoreCase(sceneStr.trim())) {
                return scene;
            }
        }
        return DEFAULT;
    }
}
