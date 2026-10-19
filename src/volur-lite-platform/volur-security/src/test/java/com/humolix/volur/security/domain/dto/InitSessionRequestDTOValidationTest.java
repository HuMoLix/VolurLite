package com.humolix.volur.security.domain.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InitSessionRequestDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidDTO_PassesValidation() {
        InitSessionRequestDTO dto = new InitSessionRequestDTO(
                "VlifWebLogin",
                "WebServiceLogin",
                "fp123456",
                System.currentTimeMillis(),
                "encrypted_data_string"
        );

        Set<ConstraintViolation<InitSessionRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void testBlankData_FailsValidation() {
        InitSessionRequestDTO dto = new InitSessionRequestDTO(
                "VlifWebLogin",
                "WebServiceLogin",
                "fp123456",
                System.currentTimeMillis(),
                ""
        );

        Set<ConstraintViolation<InitSessionRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> "data".equals(v.getPropertyPath().toString())
                && "设备参数密文不能为空".equals(v.getMessage()));
    }

    @Test
    void testNullData_FailsValidation() {
        InitSessionRequestDTO dto = new InitSessionRequestDTO(
                "VlifWebLogin",
                "WebServiceLogin",
                "fp123456",
                System.currentTimeMillis(),
                null
        );

        Set<ConstraintViolation<InitSessionRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> "data".equals(v.getPropertyPath().toString())
                && "设备参数密文不能为空".equals(v.getMessage()));
    }

}
