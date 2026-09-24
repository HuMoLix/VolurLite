package com.humolix.volur.framework.common.validation.validator;

import com.humolix.volur.framework.common.util.SpringContextHolder;
import com.humolix.volur.framework.common.validation.annotation.Email;
import com.humolix.volur.framework.common.validation.provider.EmailWhitelistProvider;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 增强型邮箱校验器
 * <p>
 * 遵循 Jakarta Bean Validation 标准规范。
 * 基础邮箱格式校验由组合继承的 {@link jakarta.validation.constraints.Email} 注解底层执行。
 * 本校验器在基础格式通过的基础上，负责根据系统配置的邮箱白名单策略进行域名后缀校验。
 * 若系统未配置白名单或配置为空，则默认放行所有合规邮箱。
 * </p>
 */
public class EmailValidator implements ConstraintValidator<Email, CharSequence> {

    private final EmailWhitelistProvider customProvider;
    private boolean checkWhitelist;

    public EmailValidator() {
        this.customProvider = null;
        this.checkWhitelist = true;
    }

    public EmailValidator(EmailWhitelistProvider customProvider, boolean checkWhitelist) {
        this.customProvider = customProvider;
        this.checkWhitelist = checkWhitelist;
    }

    @Override
    public void initialize(Email constraintAnnotation) {
        if (constraintAnnotation != null) {
            this.checkWhitelist = constraintAnnotation.checkWhitelist();
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        // 遵循 Bean Validation 规范：null 或全空白字符视为校验通过（由 @NotBlank 负责非空约束）
        if (value == null) {
            return true;
        }

        String emailStr = value.toString().trim().toLowerCase();
        if (emailStr.isEmpty()) {
            return true;
        }

        // 若无需校验白名单，直接放行（基础格式校验由组合继承的 @jakarta.validation.constraints.Email 处理）
        if (!checkWhitelist) {
            return true;
        }

        int atIndex = emailStr.lastIndexOf('@');
        // 若格式不含 @ 或末尾为 @，交由底层 @jakarta.validation.constraints.Email 拦截并抛出格式错误，此处避免重复报错
        if (atIndex <= 0 || atIndex == emailStr.length() - 1) {
            return true;
        }

        EmailWhitelistProvider provider = resolveProvider();
        if (provider == null) {
            return true;
        }

        String whitelistStr = provider.getEmailWhitelistSuffixes();
        if (!StringUtils.hasText(whitelistStr)) {
            return true;
        }

        String emailDomain = emailStr.substring(atIndex + 1);
        boolean isWhitelisted = isDomainWhitelisted(emailDomain, whitelistStr);

        if (!isWhitelisted) {
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("不支持该邮箱后缀注册，仅限: " + whitelistStr)
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }

    private EmailWhitelistProvider resolveProvider() {
        if (this.customProvider != null) {
            return this.customProvider;
        }
        return SpringContextHolder.getBean(EmailWhitelistProvider.class);
    }

    /**
     * 校验指定邮箱域名是否在白名单后缀规则中
     *
     * @param emailDomain  待校验的邮箱域名 (已小写，如 "pku.edu.cn")
     * @param whitelistStr 白名单后缀配置字符串 (如 "edu.cn, humolix.com")
     * @return true 若在白名单范围内，否则 false
     */
    public static boolean isDomainWhitelisted(String emailDomain, String whitelistStr) {
        if (!StringUtils.hasText(whitelistStr)) {
            return true;
        }
        if (!StringUtils.hasText(emailDomain)) {
            return false;
        }
        String normalizedDomain = emailDomain.trim().toLowerCase();
        return Arrays.stream(whitelistStr.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StringUtils::hasText)
                .map(suffix -> suffix.startsWith("@") ? suffix.substring(1) : suffix)
                .anyMatch(allowedDomain -> normalizedDomain.equals(allowedDomain) || normalizedDomain.endsWith("." + allowedDomain));
    }
}
