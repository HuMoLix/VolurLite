package com.humolix.volur.auth;

import com.humolix.volur.auth.domain.dto.RegisterRequestDTO;
import com.humolix.volur.framework.common.util.SpringContextHolder;
import com.humolix.volur.framework.common.validation.provider.EmailWhitelistProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestDTOValidationTest {

    private Validator validator;
    private GenericApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.applicationContext = new GenericApplicationContext();
    }

    @AfterEach
    void tearDown() {
        if (applicationContext != null && applicationContext.isActive()) {
            applicationContext.close();
        }
    }

    private RegisterRequestDTO createValidDTO() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("ValidPassword123#");
        dto.setEmail("student@pku.edu.cn");
        return dto;
    }

    @Test
    @DisplayName("邮箱格式错误触发基础 @Email 校验提示")
    void testInvalidEmailFormat_failsValidation() {
        RegisterRequestDTO dto = createValidDTO();
        dto.setEmail("invalid-email-format");

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "email".equals(v.getPropertyPath().toString())
                && v.getMessage().contains("邮箱格式不正确")));
    }

    @Test
    @DisplayName("由于平台对于邮件验证采用开关模式，DTO中未配置 NotNull/NotBlank，邮箱为空或空白字符时校验直接放行")
    void testNullOrBlankEmail_passesValidation() {
        RegisterRequestDTO dtoNull = createValidDTO();
        dtoNull.setEmail(null);
        Set<ConstraintViolation<RegisterRequestDTO>> violationsNull = validator.validate(dtoNull);
        assertTrue(violationsNull.stream().noneMatch(v -> "email".equals(v.getPropertyPath().toString())));

        RegisterRequestDTO dtoBlank = createValidDTO();
        dtoBlank.setEmail("");
        Set<ConstraintViolation<RegisterRequestDTO>> violationsBlank = validator.validate(dtoBlank);
        assertTrue(violationsBlank.stream().noneMatch(v -> "email".equals(v.getPropertyPath().toString())));
    }

    @Test
    @DisplayName("系统配置了白名单时，未在白名单的合规邮箱被拦截并提示白名单规则")
    void testEmailNotInWhitelist_failsValidation() {
        // 注册 EmailWhitelistProvider bean 到 SpringContextHolder
        applicationContext.registerBean(EmailWhitelistProvider.class, () -> () -> "edu.cn, humolix.com");
        applicationContext.refresh();
        SpringContextHolder holder = new SpringContextHolder();
        holder.setApplicationContext(applicationContext);

        RegisterRequestDTO dto = createValidDTO();
        dto.setEmail("attacker@qq.com");

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "email".equals(v.getPropertyPath().toString())
                && v.getMessage().contains("不支持该邮箱后缀注册，仅限: edu.cn, humolix.com")));
    }

    @Test
    @DisplayName("系统配置了白名单时，在白名单的合规邮箱通过校验")
    void testEmailInWhitelist_passesValidation() {
        applicationContext.registerBean(EmailWhitelistProvider.class, () -> () -> "edu.cn, humolix.com");
        applicationContext.refresh();
        SpringContextHolder holder = new SpringContextHolder();
        holder.setApplicationContext(applicationContext);

        RegisterRequestDTO dto = createValidDTO();
        dto.setEmail("student@pku.edu.cn");

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.stream().noneMatch(v -> "email".equals(v.getPropertyPath().toString())));
    }
}
