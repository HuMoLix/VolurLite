package com.humolix.volur.verification.risk.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum CaptchaType {

    LINE(1, "线段干扰验证码"),
    MATH(2, "算术计算验证码"),
    SHEAR(2, "扭曲字符验证码"),
    SLIDE_PUZZLE(3, "滑块拼图验证码");

    private final int level;
    private final String description;

    public boolean isAtLeast(CaptchaType requiredType) {
        if (requiredType == null) {
            return true;
        }
        return this.level >= requiredType.level;
    }

    public static CaptchaType fromString(String typeStr) {
        if (!StringUtils.hasText(typeStr)) {
            return null;
        }
        String cleanType = typeStr.trim().toUpperCase();
        if ("SLIDER".equals(cleanType) || "PUZZLE".equals(cleanType)) {
            return SLIDE_PUZZLE;
        }
        if ("IMAGE".equals(cleanType)) {
            return LINE;
        }
        for (CaptchaType type : values()) {
            if (type.name().equalsIgnoreCase(cleanType)) {
                return type;
            }
        }
        return null;
    }

}
