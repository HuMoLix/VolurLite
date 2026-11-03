package com.humolix.volur.security.flow.flows;

import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.domain.Flow;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SigninFlow extends Flow {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STEP_PWD = "pwd";
    public static final String STEP_AUTHENTICATE = "authenticate";
    public static final String STEP_RECAPTCHA = "recaptcha";
    public static final String STEP_2FA = "2fa";
    public static final String STEP_DEVICE = "device";
    public static final String STEP_ETOTP = "etotp";
    public static final String STEP_TOTP = "totp";

    public SigninFlow() {
        super();
        setFlowName(FlowNames.VLIF_WEB_LOGIN.getFlowName());
        setFlowEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry());
        setWaitingSteps(new ArrayList<>(List.of(STEP_PWD, STEP_AUTHENTICATE)));
    }

    public static SigninFlow create() {
        return SigninFlow.builder()
                .flowName(FlowNames.VLIF_WEB_LOGIN.getFlowName())
                .flowEntry(FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry())
                .currentStep("init")
                .cid(0)
                .completedSteps(new ArrayList<>())
                .waitingSteps(new ArrayList<>(List.of(STEP_PWD, STEP_AUTHENTICATE)))
                .build();
    }

    public static SigninFlow from(Flow flow) {
        if (flow instanceof SigninFlow signinFlow) {
            if (signinFlow.getWaitingSteps() == null || signinFlow.getWaitingSteps().isEmpty()) {
                signinFlow.setWaitingSteps(new ArrayList<>(List.of(STEP_PWD, STEP_AUTHENTICATE)));
            }
            return signinFlow;
        }
        SigninFlow signinFlow = new SigninFlow();
        if (flow != null) {
            signinFlow.setFlowName(flow.getFlowName() != null ? flow.getFlowName() : FlowNames.VLIF_WEB_LOGIN.getFlowName());
            signinFlow.setFlowEntry(flow.getFlowEntry() != null ? flow.getFlowEntry() : FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry());
            signinFlow.setIdentifier(flow.getIdentifier());
            signinFlow.setCurrentStep(flow.getCurrentStep());
            signinFlow.setCid(flow.getCid());
            signinFlow.setActiveTL(flow.getActiveTL());
            signinFlow.setCompletedSteps(flow.getCompletedSteps() != null ? new ArrayList<>(flow.getCompletedSteps()) : new ArrayList<>());
            if (flow.getWaitingSteps() != null && !flow.getWaitingSteps().isEmpty()) {
                signinFlow.setWaitingSteps(new ArrayList<>(flow.getWaitingSteps()));
            } else {
                signinFlow.setWaitingSteps(new ArrayList<>(List.of(STEP_PWD, STEP_AUTHENTICATE)));
            }
        }
        return signinFlow;
    }

}
