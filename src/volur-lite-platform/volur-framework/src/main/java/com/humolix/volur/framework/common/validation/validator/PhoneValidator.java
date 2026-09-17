package com.humolix.volur.framework.common.validation.validator;

import com.humolix.volur.framework.common.validation.annotation.Phone;
import com.humolix.volur.framework.common.validation.enums.ChinaCarrier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 中国大陆手机号码校验器（严格限定四大基础电信业务经营商号段）
 * <p>
 * 覆盖中国移动、中国联通、中国电信、中国广电四大发行商分配给公众移动通信网络的 11 位合法号段。
 * 杜绝 170/171 等虚拟运营商 (MVNO) 号段及非法号段。
 * 符合 Jakarta Validation 规范：对 null 或全空白字符视为校验通过（由 @NotBlank 负责非空约束）。
 * </p>
 */
public class PhoneValidator implements ConstraintValidator<Phone, CharSequence> {

    /**
     * 中国移动 (China Mobile) 号段正则
     * 134[0-8], 135-139, 147, 148, 150-152, 157-159, 172, 178, 182-184, 187-188, 195, 197, 198
     */
    private static final Pattern CHINA_MOBILE_PATTERN =
            Pattern.compile("^1(34[0-8]|(3[5-9]|4[78]|5[0-27-9]|7[28]|8[2-478]|9[578])\\d)\\d{7}$");

    /**
     * 中国联通 (China Unicom) 号段正则
     * 130-132, 145, 146, 155, 156, 166, 175, 176, 185, 186, 196
     */
    private static final Pattern CHINA_UNICOM_PATTERN =
            Pattern.compile("^1(3[0-2]|4[56]|5[56]|66|7[56]|8[56]|96)\\d{8}$");

    /**
     * 中国电信 (China Telecom) 号段正则
     * 133, 1349, 149, 153, 173, 174, 177, 180, 181, 189, 190, 191, 193, 199
     */
    private static final Pattern CHINA_TELECOM_PATTERN =
            Pattern.compile("^1(349|(33|49|53|7[347]|8[019]|9[0139])\\d)\\d{7}$");

    /**
     * 中国广电 (China Broadnet) 号段正则
     * 192
     */
    private static final Pattern CHINA_BROADNET_PATTERN =
            Pattern.compile("^192\\d{8}$");

    private Set<ChinaCarrier> allowedCarrierSet;

    @Override
    public void initialize(Phone constraintAnnotation) {
        ChinaCarrier[] carriers = constraintAnnotation.carriers();
        if (carriers == null || carriers.length == 0 || Arrays.asList(carriers).contains(ChinaCarrier.ALL)) {
            this.allowedCarrierSet = EnumSet.of(
                    ChinaCarrier.CHINA_MOBILE,
                    ChinaCarrier.CHINA_UNICOM,
                    ChinaCarrier.CHINA_TELECOM,
                    ChinaCarrier.CHINA_BROADNET
            );
        } else {
            this.allowedCarrierSet = EnumSet.copyOf(Arrays.asList(carriers));
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String phone = value.toString().trim();
        if (phone.isEmpty()) {
            return true;
        }
        return isValidPhone(phone, this.allowedCarrierSet);
    }

    /**
     * 探测手机号所属的运营商
     *
     * @param phone 11 位手机号
     * @return 运营商枚举，若非四大运营商号段或格式错误则返回 null
     */
    public static ChinaCarrier detectCarrier(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() != 11) {
            return null;
        }

        if (CHINA_MOBILE_PATTERN.matcher(trimmed).matches()) {
            return ChinaCarrier.CHINA_MOBILE;
        }
        if (CHINA_UNICOM_PATTERN.matcher(trimmed).matches()) {
            return ChinaCarrier.CHINA_UNICOM;
        }
        if (CHINA_TELECOM_PATTERN.matcher(trimmed).matches()) {
            return ChinaCarrier.CHINA_TELECOM;
        }
        if (CHINA_BROADNET_PATTERN.matcher(trimmed).matches()) {
            return ChinaCarrier.CHINA_BROADNET;
        }

        return null;
    }

    /**
     * 静态核验方法：校验手机号是否属于四大运营商中的任意一家
     *
     * @param phone 手机号
     * @return 是否为合法手机号
     */
    public static boolean isValidPhone(String phone) {
        return detectCarrier(phone) != null;
    }

    /**
     * 静态核验方法：校验手机号是否属于指定范围内的运营商
     *
     * @param phone           手机号
     * @param allowedCarriers 允许的运营商集合
     * @return 是否符合指定运营商要求
     */
    public static boolean isValidPhone(String phone, Set<ChinaCarrier> allowedCarriers) {
        ChinaCarrier carrier = detectCarrier(phone);
        if (carrier == null) {
            return false;
        }
        if (allowedCarriers == null || allowedCarriers.isEmpty() || allowedCarriers.contains(ChinaCarrier.ALL)) {
            return true;
        }
        return allowedCarriers.contains(carrier);
    }

    /**
     * 静态核验方法：校验手机号是否属于指定范围内的运营商
     *
     * @param phone           手机号
     * @param allowedCarriers 允许的运营商数组
     * @return 是否符合指定运营商要求
     */
    public static boolean isValidPhone(String phone, ChinaCarrier... allowedCarriers) {
        if (allowedCarriers == null || allowedCarriers.length == 0) {
            return isValidPhone(phone);
        }
        return isValidPhone(phone, EnumSet.copyOf(Arrays.asList(allowedCarriers)));
    }
}
