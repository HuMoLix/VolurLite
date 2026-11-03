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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        dto.setEmployeeNumber("20240101");
        dto.setPhone("13800138000"); // 移动合法手机号
        dto.setEmail("zhangsan@example.com");
        dto.setIdCard("110101199003072367"); // 北京合法二代身份证
        dto.setDepartment("网络安全学院");
        dto.setPosition("网安1班");
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
    @DisplayName("姓名为空白字符：触发 @NotBlank 约束")
    void testBlankRealName_failsValidation() {
        TeamMemberSignupDTO dto = createValidMember();
        dto.setRealName("   ");

        Set<ConstraintViolation<TeamMemberSignupDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "realName".equals(v.getPropertyPath().toString())
                && "姓名不能为空".equals(v.getMessage())));
    }

    @Test
    @DisplayName("指导老师/领队使用 TeamLeadersSignupDTO 必填姓名与手机号：校验通过")
    void testLeaderWithRequiredFields_passesValidation() {
        TeamLeadersSignupDTO dto = new TeamLeadersSignupDTO();
        dto.setRealName("李老师");
        dto.setPhone("13800138000");
        dto.setGender(Gender.MALE);
        // 不填 email, idCard, employeeNumber, department, position
        Set<ConstraintViolation<TeamLeadersSignupDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "指导老师提供必填姓名和手机号应通过 DTO 校验");
    }

    @Test
    @DisplayName("指导老师/领队未填姓名或手机号：触发校验失败")
    void testLeaderWithoutRequiredFields_failsValidation() {
        TeamLeadersSignupDTO dto = new TeamLeadersSignupDTO();
        Set<ConstraintViolation<TeamLeadersSignupDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "指导老师未填姓名和手机号应触发校验失败");
        assertTrue(violations.stream().anyMatch(v -> "realName".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "phone".equals(v.getPropertyPath().toString())));
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
