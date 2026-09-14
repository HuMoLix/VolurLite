package com.humolix.volur.framework.common.util;

import org.springframework.util.StringUtils;

/**
 * 全局数据脱敏工具类
 * 提供对身份证号、手机号、电子邮箱、真实姓名、银行卡、API密钥等关键敏感信息的统一掩码脱敏处理。
 */
public final class DesensitizationUtil {

    private static final String DEFAULT_MASK = "***";
    private static final char DEFAULT_MASK_CHAR = '*';

    private DesensitizationUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 手机号脱敏
     * 11 位大陆手机号保留前 3 后 4，中间使用 4 个 '*' 隐藏（如 138****5678）
     * 其它长度若大于 7 位保留前 3 后 4，否则返回通用脱敏字符
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String trimmed = phone.trim();
        int len = trimmed.length();
        if (len == 11) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(7);
        }
        if (len > 7) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(len - 4);
        }
        return DEFAULT_MASK;
    }

    /**
     * 身份证号脱敏
     * 18 位二代身份证保留前 6 后 4，中间使用 8 个 '*' 隐藏（如 110101********1234）
     * 15 位一代身份证保留前 6 后 3，中间使用 6 个 '*' 隐藏（如 110101******123）
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return idCard;
        }
        String trimmed = idCard.trim();
        int len = trimmed.length();
        if (len == 18) {
            return trimmed.substring(0, 6) + "********" + trimmed.substring(14);
        }
        if (len == 15) {
            return trimmed.substring(0, 6) + "******" + trimmed.substring(12);
        }
        if (len >= 8) {
            return trimmed.substring(0, 4) + "****" + trimmed.substring(len - 4);
        }
        return DEFAULT_MASK;
    }

    /**
     * 电子邮箱脱敏
     * 用户名部分保留首尾字符，中间以 '***' 隐藏（如 z***n@example.com）
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return DEFAULT_MASK;
        }
        String name = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    /**
     * 真实姓名脱敏
     * 1 个字符：*
     * 2 个字符：张*
     * 3 个字符：李*五
     * 4 个及以上字符：诸**明（保留首尾，中间全掩码）
     *
     * @param realName 原始姓名
     * @return 脱敏后的姓名
     */
    public static String maskRealName(String realName) {
        if (!StringUtils.hasText(realName)) {
            return realName;
        }
        String trimmed = realName.trim();
        int len = trimmed.length();
        if (len == 1) {
            return "*";
        }
        if (len == 2) {
            return trimmed.charAt(0) + "*";
        }
        if (len == 3) {
            return trimmed.charAt(0) + "*" + trimmed.charAt(2);
        }
        return trimmed.charAt(0) + "*".repeat(len - 2) + trimmed.charAt(len - 1);
    }

    /**
     * 银行卡号脱敏
     * 保留前 6 位和后 4 位，中间以 '*' 掩码（如 622202************1234）
     *
     * @param cardNum 原始银行卡号
     * @return 脱敏后的卡号
     */
    public static String maskBankCard(String cardNum) {
        if (!StringUtils.hasText(cardNum)) {
            return cardNum;
        }
        String trimmed = cardNum.trim().replaceAll("\\s+", "");
        int len = trimmed.length();
        if (len <= 10) {
            return DEFAULT_MASK;
        }
        return trimmed.substring(0, 6) + "*".repeat(len - 10) + trimmed.substring(len - 4);
    }

    /**
     * 密钥/敏感 Token 脱敏
     * 长度大于 8 时保留前 4 后 4，中间以 8 个 '*' 掩码，否则返回全掩码
     *
     * @param key 原始密钥
     * @return 脱敏后的密钥
     */
    public static String maskKey(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        String trimmed = key.trim();
        int len = trimmed.length();
        if (len > 8) {
            return trimmed.substring(0, 4) + "********" + trimmed.substring(len - 4);
        }
        return "********";
    }

    /**
     * 通用字符串自定义掩码脱敏
     *
     * @param str 原始字符串
     * @param prefixKeep 前面保留字符数
     * @param suffixKeep 后面保留字符数
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixKeep, int suffixKeep, char maskChar) {
        if (!StringUtils.hasText(str)) {
            return str;
        }
        int len = str.length();
        if (prefixKeep < 0) prefixKeep = 0;
        if (suffixKeep < 0) suffixKeep = 0;
        if (prefixKeep + suffixKeep >= len) {
            return str;
        }
        return str.substring(0, prefixKeep) +
                String.valueOf(maskChar).repeat(len - prefixKeep - suffixKeep) +
                str.substring(len - suffixKeep);
    }

    /**
     * 通用字符串自定义掩码脱敏（默认使用 '*' 字符）
     *
     * @param str 原始字符串
     * @param prefixKeep 前面保留字符数
     * @param suffixKeep 后面保留字符数
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixKeep, int suffixKeep) {
        return mask(str, prefixKeep, suffixKeep, DEFAULT_MASK_CHAR);
    }

}
