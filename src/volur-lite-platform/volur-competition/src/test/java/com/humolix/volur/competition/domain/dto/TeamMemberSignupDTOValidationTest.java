package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TeamMemberSignupDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private TeamMemberSignupDTO createValidMember() {
        TeamMemberSignupDTO dto = new TeamMemberSignupDTO();
        dto.setRealName("张三");
        dto.setGender(Gender.MALE);
        dto.setRole(MemberRole.MEMBER);
        dto.setStudentNumber("20240101");
        dto.setPhone("13800138000"); // 移动合法手机号
        dto.setEmail("zhangsan@example.com");
        dto.setIdCard("110101199003072367"); // 北京合法二代身份证
        dto.setCollege("网络安全学院");
        return dto;
    }

    @Test
    @DisplayName("完整合法报名人员信息：校验通过无约束违规")
    void testValidMember_passesValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "合法的成员报名信息不应产生校验失败");
    }

    @Test
    @DisplayName("手机号格式不正确（如 170 虚商或非法号段）：触发 @Phone 校验失败")
    void testInvalidPhone_failsValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        dto.setPhone("17001234567"); // 170 虚拟运营商号段，非四大运营商公众移动号段

        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "phone".equals(v.getPropertyPath().toString())
                && "手机号格式不正确".equals(v.getMessage())));
    }

    @Test
    @DisplayName("身份证格式不正确（如校验码错误、非法省份或生日）：触发 @IdentificationCard 校验失败")
    void testInvalidIdCard_failsValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        dto.setIdCard("110101199003072368"); // 真实校验码应为 7，末位为 8 必被拦截

        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "idCard".equals(v.getPropertyPath().toString())
                && "身份证号格式不正确".equals(v.getMessage())));
    }

    @Test
    @DisplayName("身份证与手机号为空白字符：同时触发 @NotBlank 约束")
    void testBlankPhoneAndIdCard_failsValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        dto.setPhone("   ");
        dto.setIdCard("");

        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "phone".equals(v.getPropertyPath().toString())
                && "手机号不能为空".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "idCard".equals(v.getPropertyPath().toString())
                && "身份证号不能为空".equals(v.getMessage())));
    }

    @Test
    @DisplayName("末位为 X 的有效身份证测试通过")
    void testValidIdCardWithX_passesValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        dto.setIdCard("11010119900301129X");
        dto.setPhone("19212345678"); // 广电号段

        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "合法的 X 结尾身份证和广电手机号应校验通过");
    }
}
