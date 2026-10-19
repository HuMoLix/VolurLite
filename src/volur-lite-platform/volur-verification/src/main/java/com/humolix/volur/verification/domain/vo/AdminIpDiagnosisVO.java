package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIpDiagnosisVO {

    private String ip;

    @Builder.Default
    private Boolean isBanned = false;

    @Builder.Default
    private Boolean isWhitelisted = false;

    @Builder.Default
    private Boolean isBypassed = false;

    @Builder.Default
    private String riskLevel = "LOW";

    @Builder.Default
    private Long recentFailures = 0L;

    @Builder.Default
    private Long recentVelocity = 0L;

    private String banReason;

    private Instant banExpiresAt;

    private String banSource;

    private String deviceFingerprint;

    private String browserFamily;

    private String tlsProtocol;

    private String tlsCipher;

    private String tlsAlpn;

}
