package com.humolix.volur.security.flow.domain;

import com.humolix.volur.framework.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowNamesTest {

    @Test
    void from_withValidFlowNames_shouldReturnEnum() {
        assertThat(FlowNames.from("VlifWebLogin")).isEqualTo(FlowNames.VLIF_WEB_LOGIN);
        assertThat(FlowNames.from("VLIF_WEB_LOGIN")).isEqualTo(FlowNames.VLIF_WEB_LOGIN);
        assertThat(FlowNames.from("vlifweblogin")).isEqualTo(FlowNames.VLIF_WEB_LOGIN);
        assertThat(FlowNames.from("VlifWebSignUp")).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);
        assertThat(FlowNames.from("VLIF_WEB_SIGNUP")).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);
        assertThat(FlowNames.from("vlifwebsignup")).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);
    }

    @Test
    void from_withInvalidOrBlank_shouldThrowException() {
        assertThatThrownBy(() -> FlowNames.from(null))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.from(""))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.from("UnknownFlow"))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void isValidFlowName_shouldReturnCorrectBoolean() {
        assertThat(FlowNames.isValidFlowName("VlifWebLogin")).isTrue();
        assertThat(FlowNames.isValidFlowName("VlifWebSignUp")).isTrue();
        assertThat(FlowNames.isValidFlowName("VLIF_WEB_LOGIN")).isTrue();
        assertThat(FlowNames.isValidFlowName("invalid_flow")).isFalse();
        assertThat(FlowNames.isValidFlowName(null)).isFalse();
        assertThat(FlowNames.isValidFlowName("")).isFalse();
    }

    @Test
    void flowEntry_from_withValidEntries_shouldReturnEnum() {
        assertThat(FlowNames.FlowEntry.from("WebServiceLogin")).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN);
        assertThat(FlowNames.FlowEntry.from("WEB_SERVICE_LOGIN")).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN);
        assertThat(FlowNames.FlowEntry.from("WebSignUp")).isEqualTo(FlowNames.FlowEntry.WEB_SIGN_UP);
        assertThat(FlowNames.FlowEntry.from("WEB_SIGN_UP")).isEqualTo(FlowNames.FlowEntry.WEB_SIGN_UP);
    }

    @Test
    void flowEntry_from_withInvalidOrBlank_shouldThrowException() {
        assertThatThrownBy(() -> FlowNames.FlowEntry.from(null))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.FlowEntry.from("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.FlowEntry.from("NoSuchEntry"))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void flowEntry_isValid_shouldReturnCorrectBoolean() {
        assertThat(FlowNames.FlowEntry.isValid("WebServiceLogin")).isTrue();
        assertThat(FlowNames.FlowEntry.isValid("WebSignUp")).isTrue();
        assertThat(FlowNames.FlowEntry.isValid("unknown")).isFalse();
        assertThat(FlowNames.FlowEntry.isValid(null)).isFalse();
    }

    @Test
    void flowEntry_getFlowNames_shouldReturnParentFlow() {
        assertThat(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getFlowNames()).isEqualTo(FlowNames.VLIF_WEB_LOGIN);
        assertThat(FlowNames.FlowEntry.WEB_SIGN_UP.getFlowNames()).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);

        assertThat(FlowNames.fromEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN)).isEqualTo(FlowNames.VLIF_WEB_LOGIN);
        assertThat(FlowNames.fromEntry(FlowNames.FlowEntry.WEB_SIGN_UP)).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);
    }

    @Test
    void flowNames_hasEntry_and_findEntry_shouldSucceedForMatchingEntry() {
        FlowNames loginFlow = FlowNames.VLIF_WEB_LOGIN;
        assertThat(loginFlow.hasEntry("WebServiceLogin")).isTrue();
        assertThat(loginFlow.hasEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN)).isTrue();
        assertThat(loginFlow.findEntry("WebServiceLogin")).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN);
        assertThat(loginFlow.getDefaultEntry()).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN);

        FlowNames signupFlow = FlowNames.VLIF_WEB_SIGNUP;
        assertThat(signupFlow.hasEntry("WebSignUp")).isTrue();
        assertThat(signupFlow.hasEntry(FlowNames.FlowEntry.WEB_SIGN_UP)).isTrue();
        assertThat(signupFlow.findEntry("WebSignUp")).isEqualTo(FlowNames.FlowEntry.WEB_SIGN_UP);
        assertThat(signupFlow.getDefaultEntry()).isEqualTo(FlowNames.FlowEntry.WEB_SIGN_UP);
    }

    @Test
    void flowNames_hasEntry_and_findEntry_shouldFailForMismatchedEntry() {
        FlowNames loginFlow = FlowNames.VLIF_WEB_LOGIN;
        assertThat(loginFlow.hasEntry("WebSignUp")).isFalse();
        assertThat(loginFlow.hasEntry(FlowNames.FlowEntry.WEB_SIGN_UP)).isFalse();

        assertThatThrownBy(() -> loginFlow.findEntry("WebSignUp"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于流程")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        FlowNames signupFlow = FlowNames.VLIF_WEB_SIGNUP;
        assertThat(signupFlow.hasEntry("WebServiceLogin")).isFalse();
        assertThat(signupFlow.hasEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN)).isFalse();

        assertThatThrownBy(() -> signupFlow.findEntry("WebServiceLogin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于流程")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validate_withMatchingPair_shouldSucceed() {
        FlowNames flow1 = FlowNames.validate("VlifWebLogin", "WebServiceLogin");
        assertThat(flow1).isEqualTo(FlowNames.VLIF_WEB_LOGIN);

        FlowNames flow2 = FlowNames.validate("VlifWebSignUp", "WebSignUp");
        assertThat(flow2).isEqualTo(FlowNames.VLIF_WEB_SIGNUP);

        assertThat(FlowNames.isValid("VlifWebLogin", "WebServiceLogin")).isTrue();
        assertThat(FlowNames.isValid("VlifWebSignUp", "WebSignUp")).isTrue();
    }

    @Test
    void validate_withMismatchedPair_shouldThrowException() {
        assertThatThrownBy(() -> FlowNames.validate("VlifWebLogin", "WebSignUp"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于流程")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.validate("VlifWebSignUp", "WebServiceLogin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于流程")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> FlowNames.validate("UnknownFlow", "WebServiceLogin"))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(FlowNames.isValid("VlifWebLogin", "WebSignUp")).isFalse();
        assertThat(FlowNames.isValid("VlifWebSignUp", "WebServiceLogin")).isFalse();
        assertThat(FlowNames.isValid("UnknownFlow", "WebServiceLogin")).isFalse();
        assertThat(FlowNames.isValid(null, "WebServiceLogin")).isFalse();
        assertThat(FlowNames.isValid("VlifWebLogin", null)).isFalse();
    }

}
