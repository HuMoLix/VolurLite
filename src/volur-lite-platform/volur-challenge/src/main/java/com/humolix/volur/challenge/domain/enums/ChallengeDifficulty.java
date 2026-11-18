package com.humolix.volur.challenge.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.humolix.volur.framework.domain.enums.EnumEntity;

/**
 * CTF 赛题难度等级枚举
 * <p>
 * 参考 {@link ChallengeType} 规范设计，实现 {@link EnumEntity} 接口：
 * <ul>
 *   <li>1 - EASY: 入门 (easy)</li>
 *   <li>2 - NORMAL: 简单 (normal)</li>
 *   <li>3 - MID: 中等 (mid)</li>
 *   <li>4 - HARD: 进阶 (hard)</li>
 *   <li>5 - DIFFICULT: 困难 (difficult)</li>
 * </ul>
 */
public enum ChallengeDifficulty implements EnumEntity {

    EASY(1, "easy", "入门"),
    NORMAL(2, "normal", "简单"),
    MID(3, "mid", "中等"),
    HARD(4, "hard", "进阶"),
    DIFFICULT(5, "difficult", "困难");

    private final int code;
    @JsonValue
    private final String description;
    private final String label;

    ChallengeDifficulty(int code, String description, String label) {
        this.code = code;
        this.description = description;
        this.label = label;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ChallengeDifficulty fromValue(Object value) {
        return fromValueStrict(value).orElse(EASY);
    }

    /**
     * 严格解析：识别枚举名/描述值/中文别名，未识别时返回 empty（不回退默认值）。
     * 供筛选类接口校验入参，避免拼写错误被静默映射为 EASY。
     */
    public static java.util.Optional<ChallengeDifficulty> fromValueStrict(Object value) {
        if (value == null) {
            return java.util.Optional.empty();
        }
        if (value instanceof Number num) {
            return java.util.Optional.of(fromCode(num.intValue()));
        }
        String str = value.toString().trim().toLowerCase();
        for (ChallengeDifficulty diff : values()) {
            if (diff.description.equalsIgnoreCase(str) || diff.name().equalsIgnoreCase(str)) {
                return java.util.Optional.of(diff);
            }
        }
        return switch (str) {
            case "入门", "签到", "baby" -> java.util.Optional.of(EASY);
            case "简单" -> java.util.Optional.of(NORMAL);
            case "中等", "medium" -> java.util.Optional.of(MID);
            case "进阶" -> java.util.Optional.of(HARD);
            case "困难", "硬核", "hardcore" -> java.util.Optional.of(DIFFICULT);
            default -> java.util.Optional.empty();
        };
    }

    public static ChallengeDifficulty fromCode(int code) {
        for (ChallengeDifficulty diff : values()) {
            if (diff.code == code) {
                return diff;
            }
        }
        return EASY;
    }

}
