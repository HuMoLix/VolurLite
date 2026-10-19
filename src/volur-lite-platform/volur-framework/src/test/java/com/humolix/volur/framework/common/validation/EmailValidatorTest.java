package com.humolix.volur.framework.common.validation;

import com.humolix.volur.framework.common.validation.annotation.Email;
import com.humolix.volur.framework.common.validation.provider.EmailWhitelistProvider;
import com.humolix.volur.framework.common.validation.validator.EmailValidator;
import jakarta.validation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class EmailValidatorTest {

    static class TestUserDTO {
        @Email(message = "邮箱格式不正确")
        private String email;

        public TestUserDTO(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }
    }

    @Test
    @DisplayName("空值或空白字符根据规范直接放行（由 @NotBlank 约束）")
    void testNullOrBlankEmail_passes() {
        EmailValidator validator = new EmailValidator();
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    @DisplayName("白名单为空时放行所有合规邮箱")
    void testEmptyWhitelist_passesAll() {
        EmailWhitelistProvider provider = () -> "";
        EmailValidator validator = new EmailValidator(provider, true);

        assertTrue(validator.isValid("user@gmail.com", null));
        assertTrue(validator.isValid("user@pku.edu.cn", null));
        assertTrue(validator.isValid("admin@volur.local", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "student@pku.edu.cn",
            "teacher@tsinghua.edu.cn",
            "admin@humolix.com",
            "dev@sub.humolix.com",
            "user@MAIL.HUMOLIX.COM"
    })
    @DisplayName("命中白名单后缀的邮箱校验通过")
    void testWhitelistedEmails_pass(String email) {
        EmailWhitelistProvider provider = () -> "edu.cn, @humolix.com";
        EmailValidator validator = new EmailValidator(provider, true);

        assertTrue(validator.isValid(email, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "attacker@qq.com",
            "user@163.com",
            "test@gmail.com",
            "fake@edu.cn.malicious.com"
    })
    @DisplayName("未命中白名单后缀的邮箱校验失败并设置违规提示")
    void testNonWhitelistedEmails_fail(String email) {
        String whitelist = "edu.cn, @humolix.com";
        EmailWhitelistProvider provider = () -> whitelist;
        EmailValidator validator = new EmailValidator(provider, true);

        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);

        assertFalse(validator.isValid(email, context));
        Mockito.verify(context).disableDefaultConstraintViolation();
        Mockito.verify(context).buildConstraintViolationWithTemplate("不支持该邮箱后缀注册，仅限: " + whitelist);
        Mockito.verify(builder).addConstraintViolation();
    }

    @Test
    @DisplayName("关闭白名单校验时放行")
    void testDisableWhitelist_passes() {
        EmailWhitelistProvider provider = () -> "edu.cn";
        EmailValidator validator = new EmailValidator(provider, false);

        assertTrue(validator.isValid("user@gmail.com", null));
    }

    @Test
    @DisplayName("isDomainWhitelisted 静态工具方法逻辑验证")
    void testIsDomainWhitelistedStaticMethod() {
        // 空白名单或空域名
        assertTrue(EmailValidator.isDomainWhitelisted("pku.edu.cn", ""));
        assertTrue(EmailValidator.isDomainWhitelisted("pku.edu.cn", null));
        assertFalse(EmailValidator.isDomainWhitelisted("", "edu.cn"));
        assertFalse(EmailValidator.isDomainWhitelisted(null, "edu.cn"));

        // 前缀 @ 与空格兼容
        assertTrue(EmailValidator.isDomainWhitelisted("pku.edu.cn", "  @edu.cn  ,  humolix.com "));
        assertTrue(EmailValidator.isDomainWhitelisted("humolix.com", "  @edu.cn  ,  humolix.com "));
        assertTrue(EmailValidator.isDomainWhitelisted("sub.humolix.com", "  @edu.cn  ,  humolix.com "));

        // 包含但不是子域或同名
        assertFalse(EmailValidator.isDomainWhitelisted("notedu.cn", "edu.cn"));
        assertFalse(EmailValidator.isDomainWhitelisted("fakehumolix.com", "humolix.com"));
    }

    @Test
    @DisplayName("结合 Jakarta Validator 验证继承的标准 @Email 格式校验能力")
    void testJakartaValidatorComposition() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 非法格式邮箱，基础 @Email 应当触发违规
        TestUserDTO invalidDTO = new TestUserDTO("not-an-email");
        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate(invalidDTO);
        assertFalse(violations.isEmpty(), "不合法的邮箱格式应当被拦截");
        assertTrue(violations.stream().anyMatch(v -> "邮箱格式不正确".equals(v.getMessage())));

        // 合法格式邮箱（无 Spring 上下文时默认白名单无限制放行）
        TestUserDTO validDTO = new TestUserDTO("test@volur.local");
        Set<ConstraintViolation<TestUserDTO>> validViolations = validator.validate(validDTO);
        assertTrue(validViolations.isEmpty(), "合法格式邮箱应通过基础校验");
    }
}
