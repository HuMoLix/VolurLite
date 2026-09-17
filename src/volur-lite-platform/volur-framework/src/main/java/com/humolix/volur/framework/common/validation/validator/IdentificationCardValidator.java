package com.humolix.volur.framework.common.validation.validator;

import com.humolix.volur.framework.common.validation.annotation.IdentificationCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 中国大陆第二代居民身份证有效性校验器 (GB 11643-1999)
 * <p>
 * 严格按照国家标准 GB 11643-1999 进行 18 位第二代公民身份证的格式、行政区划、出生日期及加权校验码计算核验。
 * 符合 Jakarta Validation 规范：对 null 或全空白字符视为校验通过（由 @NotBlank 负责非空约束）。
 * </p>
 */
public class IdentificationCardValidator implements ConstraintValidator<IdentificationCard, CharSequence> {

    /**
     * 18 位二代身份证基础格式正则（首位非 0，年份 18/19/20 开头，月份 01-12，日期 01-31，后三位数字，末位数字或 X/x）
     */
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    /**
     * GB/T 2260 中华人民共和国行政区划代码前两位（省/自治区/直辖市/特别行政区代码）
     */
    private static final Set<String> PROVINCE_CODES = Set.of(
            // 华北
            "11", "12", "13", "14", "15",
            // 东北
            "21", "22", "23",
            // 华东
            "31", "32", "33", "34", "35", "36", "37",
            // 中南
            "41", "42", "43", "44", "45", "46",
            // 西南
            "50", "51", "52", "53", "54",
            // 西北
            "61", "62", "63", "64", "65",
            // 港澳台
            "71", "81", "82"
    );

    /**
     * ISO 7064:1983.MOD 11-2 加权因子数组（前 17 位权重）
     */
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /**
     * 模 11 余数对应的第 18 位校验码字符映射表
     */
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String idCard = value.toString().trim();
        if (idCard.isEmpty()) {
            return true;
        }
        return isValidIdCard(idCard);
    }

    /**
     * 静态核验方法：校验 18 位第二代身份证有效性
     *
     * @param idCard 身份证号码
     * @return 是否为合法有效的二代身份证
     */
    public static boolean isValidIdCard(String idCard) {
        if (idCard == null) {
            return false;
        }
        String trimmed = idCard.trim();
        if (trimmed.length() != 18) {
            return false;
        }

        // 1. 正则预检
        if (!ID_CARD_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 2. 省份行政区划代码前两位校验
        String provinceCode = trimmed.substring(0, 2);
        if (!PROVINCE_CODES.contains(provinceCode)) {
            return false;
        }

        // 3. 出生日期合法性校验
        try {
            int year = Integer.parseInt(trimmed.substring(6, 10));
            int month = Integer.parseInt(trimmed.substring(10, 12));
            int day = Integer.parseInt(trimmed.substring(12, 14));

            LocalDate now = LocalDate.now();
            if (year < 1900 || year > now.getYear()) {
                return false;
            }

            LocalDate birthDate = LocalDate.of(year, month, day);
            if (birthDate.isAfter(now)) {
                return false;
            }
        } catch (DateTimeException | NumberFormatException e) {
            return false;
        }

        // 4. ISO 7064:1983.MOD 11-2 校验码计算核验
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (trimmed.charAt(i) - '0') * WEIGHTS[i];
        }
        int remainder = sum % 11;
        char expectedCheckCode = CHECK_CODES[remainder];
        char actualCheckCode = Character.toUpperCase(trimmed.charAt(17));

        return expectedCheckCode == actualCheckCode;
    }

    /**
     * 从合法身份证号中提取出生日期
     *
     * @param idCard 身份证号码
     * @return 出生日期，若不合法则返回 null
     */
    public static LocalDate extractBirthDate(String idCard) {
        if (!isValidIdCard(idCard)) {
            return null;
        }
        String trimmed = idCard.trim();
        int year = Integer.parseInt(trimmed.substring(6, 10));
        int month = Integer.parseInt(trimmed.substring(10, 12));
        int day = Integer.parseInt(trimmed.substring(12, 14));
        return LocalDate.of(year, month, day);
    }

    /**
     * 从合法身份证号中提取性别判定结果 (第 17 位奇数为男，偶数为女)
     *
     * @param idCard 身份证号码
     * @return true 表示男性，false 表示女性，不合法返回 null
     */
    public static Boolean isMale(String idCard) {
        if (!isValidIdCard(idCard)) {
            return null;
        }
        String trimmed = idCard.trim();
        int genderCode = trimmed.charAt(16) - '0';
        return (genderCode % 2) != 0;
    }
}
