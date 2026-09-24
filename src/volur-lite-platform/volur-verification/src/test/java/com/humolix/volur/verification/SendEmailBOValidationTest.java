package com.humolix.volur.verification;

import com.humolix.volur.verification.domain.bo.SendEmailBO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SendEmailBOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("测试 SendEmailBO 正常上下文注入与必填字段验证通过")
    void testSendEmailBO_Valid() {
        SendEmailBO bo = SendEmailBO.builder()
                .to("user@example.com")
                .subject("测试主题")
                .content("测试内容")
                .build();

        Set<ConstraintViolation<SendEmailBO>> violations = validator.validate(bo);
        assertThat(violations).isEmpty();
        assertThat(bo.getOperatorId()).isNotNull();
        assertThat(bo.getOperatorIp()).isNotBlank();
    }

    @Test
    @DisplayName("测试 SendEmailBO 缺失必填字段时违反约束")
    void testSendEmailBO_InvalidFields() {
        SendEmailBO bo = SendEmailBO.builder()
                .to("")
                .subject("")
                .build();

        Set<ConstraintViolation<SendEmailBO>> violations = validator.validate(bo);
        assertThat(violations).isNotEmpty();

        Set<String> propertyPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(propertyPaths).contains("to", "subject");
    }

    @Test
    @DisplayName("测试 SendEmailBO 邮箱格式不正确时违反约束")
    void testSendEmailBO_InvalidEmailFormat() {
        SendEmailBO bo = SendEmailBO.builder()
                .to("not-an-email")
                .subject("有效主题")
                .build();

        Set<ConstraintViolation<SendEmailBO>> violations = validator.validate(bo);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "to".equals(v.getPropertyPath().toString()))).isTrue();
    }
}
