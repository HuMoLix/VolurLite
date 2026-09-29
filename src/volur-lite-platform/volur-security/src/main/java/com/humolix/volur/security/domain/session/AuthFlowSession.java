package com.humolix.volur.security.domain.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthFlowSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dsh;

    @Builder.Default
    private String flowName = "VlifWebLogin";

    @Builder.Default
    private String flowEntry = "WebServiceLogin";

    private String identifier;

    @Builder.Default
    private String currentStep = "init";

    @Builder.Default
    private Integer cid = 0;

    private String activeTL;

    private String clientIp;

    private String userAgent;

    private String deviceFingerprint;

    private String riskLevel;

    @Builder.Default
    private List<String> completedSteps = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Builder.Default
    private Long updatedAt = System.currentTimeMillis();

}
