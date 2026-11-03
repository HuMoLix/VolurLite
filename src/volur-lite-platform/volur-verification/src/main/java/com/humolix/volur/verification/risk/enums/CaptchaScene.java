package com.humolix.volur.verification.risk.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum CaptchaScene {

    LOGIN(CaptchaType.LINE, "用户登录场景"),
    REGISTER(CaptchaType.SLIDE_PUZZLE, "用户注册场景"),
    CHANGE_PASSWORD(CaptchaType.SLIDE_PUZZLE, "修改密码场景"),
    RESET_PASSWORD(CaptchaType.SLIDE_PUZZLE, "找回重置密码场景"),
    SENSITIVE_OP(CaptchaType.SLIDE_PUZZLE, "高危敏感操作场景"),
    DEFAULT(CaptchaType.LINE, "通用默认场景");

    private final CaptchaType minRequiredType;
    private final String description;

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
