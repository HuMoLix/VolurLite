package com.humolix.volur.security.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyCredentialVO implements Serializable {

    private String credentialId;

    private String label;

    private Instant created;

    private Instant lastUsed;

    private Long signatureCount;

    private String authenticatorTransports;

}
