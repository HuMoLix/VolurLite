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
        if (value == null) {
            return null;
        }
        if (value instanceof Number num) {
            return fromCode(num.intValue());
        }
        String str = value.toString().trim().toLowerCase();
        for (ChallengeDifficulty diff : values()) {
            if (diff.description.equalsIgnoreCase(str) || diff.name().equalsIgnoreCase(str)) {
                return diff;
            }
        }
        return switch (str) {
            case "入门", "签到", "baby" -> EASY;
            case "简单" -> NORMAL;
            case "中等", "medium" -> MID;
            case "进阶" -> HARD;
            case "困难", "硬核", "hardcore" -> DIFFICULT;
            default -> EASY;
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
