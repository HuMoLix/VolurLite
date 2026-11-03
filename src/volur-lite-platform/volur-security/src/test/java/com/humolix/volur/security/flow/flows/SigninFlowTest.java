package com.humolix.volur.security.flow.flows;

import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.domain.Flow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SigninFlowTest {

    @Test
    void constructor_shouldInitializeDefaults() {
        SigninFlow flow = new SigninFlow();
        assertThat(flow.getFlowName()).isEqualTo(FlowNames.VLIF_WEB_LOGIN.getFlowName());
        assertThat(flow.getFlowEntry()).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry());
        assertThat(flow.getWaitingSteps()).containsExactly("pwd", "authenticate");
        assertThat(flow.getCompletedSteps()).isEmpty();
    }

    @Test
    void create_shouldBuildValidSigninFlow() {
        SigninFlow flow = SigninFlow.create();
        assertThat(flow.getFlowName()).isEqualTo(FlowNames.VLIF_WEB_LOGIN.getFlowName());
        assertThat(flow.getFlowEntry()).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry());
        assertThat(flow.getCurrentStep()).isEqualTo("init");
        assertThat(flow.getCid()).isEqualTo(0);
        assertThat(flow.getWaitingSteps()).containsExactly("pwd", "authenticate");
    }

    @Test
    void from_withGenericFlow_shouldPreserveFieldsAndInitializeWaitingSteps() {
        Flow genericFlow = Flow.builder()
                .flowName(FlowNames.VLIF_WEB_LOGIN.getFlowName())
                .flowEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry())
                .identifier("alice")
                .currentStep("identifier")
                .cid(1)
                .activeTL("ADG-token")
                .completedSteps(new ArrayList<>(List.of("init")))
                .waitingSteps(new ArrayList<>())
                .build();

        SigninFlow signinFlow = SigninFlow.from(genericFlow);
        assertThat(signinFlow.getFlowName()).isEqualTo(FlowNames.VLIF_WEB_LOGIN.getFlowName());
        assertThat(signinFlow.getFlowEntry()).isEqualTo(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry());
        assertThat(signinFlow.getIdentifier()).isEqualTo("alice");
        assertThat(signinFlow.getCurrentStep()).isEqualTo("identifier");
        assertThat(signinFlow.getCid()).isEqualTo(1);
        assertThat(signinFlow.getActiveTL()).isEqualTo("ADG-token");
        assertThat(signinFlow.getCompletedSteps()).containsExactly("init");
        assertThat(signinFlow.getWaitingSteps()).containsExactly("pwd", "authenticate");
    }

    @Test
    void from_withExistingSigninFlow_shouldReturnSame() {
        SigninFlow original = SigninFlow.create();
        SigninFlow resolved = SigninFlow.from(original);
        assertThat(resolved).isSameAs(original);
    }

    @Test
    void waitingSteps_management_addWaitingStepFirst_and_completeStep() {
        SigninFlow flow = SigninFlow.create();
        assertThat(flow.getWaitingSteps()).containsExactly("pwd", "authenticate");

        flow.addWaitingStepFirst("recaptcha");
        assertThat(flow.getWaitingSteps()).containsExactly("recaptcha", "pwd", "authenticate");

        flow.completeStep("recaptcha");
        assertThat(flow.getWaitingSteps()).containsExactly("pwd", "authenticate");
        assertThat(flow.getCompletedSteps()).containsExactly("recaptcha");

        flow.completeStep("pwd");
        assertThat(flow.getWaitingSteps()).containsExactly("authenticate");
        assertThat(flow.getCompletedSteps()).containsExactly("recaptcha", "pwd");

        flow.completeStep("authenticate");
        assertThat(flow.getWaitingSteps()).isEmpty();
        assertThat(flow.getCompletedSteps()).containsExactly("recaptcha", "pwd", "authenticate");
    }

}
