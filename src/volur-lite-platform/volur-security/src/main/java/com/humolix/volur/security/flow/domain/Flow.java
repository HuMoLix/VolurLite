package com.humolix.volur.security.flow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Flow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String flowName;

    private String flowEntry;

    private String identifier;

    private String currentStep;

    @Builder.Default
    private Integer cid = 0;

    private String activeTL;

    @Builder.Default
    private List<String> completedSteps = new ArrayList<>();

    @Builder.Default
    private List<String> waitingSteps = new ArrayList<>();

    public void addWaitingStep(String step) {
        if (this.waitingSteps == null) {
            this.waitingSteps = new ArrayList<>();
        }
        if (step != null && !this.waitingSteps.contains(step)) {
            this.waitingSteps.add(step);
        }
    }

    public void addWaitingStepFirst(String step) {
        if (this.waitingSteps == null) {
            this.waitingSteps = new ArrayList<>();
        }
        if (step != null) {
            this.waitingSteps.remove(step);
            this.waitingSteps.addFirst(step);
        }
    }

    public void completeStep(String step) {
        if (this.waitingSteps != null && step != null) {
            this.waitingSteps.remove(step);
        }
        if (this.completedSteps == null) {
            this.completedSteps = new ArrayList<>();
        }
        if (step != null && !this.completedSteps.contains(step)) {
            this.completedSteps.add(step);
        }
    }

}
