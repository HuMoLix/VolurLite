package com.humolix.volur.security.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_credentials", indexes = {
        @Index(name = "idx_user_credentials_user_id", columnList = "user_entity_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "credential_id", nullable = false, length = 255)
    private String credentialId;

    @Column(name = "user_entity_user_id", nullable = false, length = 255)
    private String userEntityUserId;

    @Lob
    @Column(name = "public_key", nullable = false, columnDefinition = "BLOB")
    private byte[] publicKey;

    @Column(name = "signature_count", nullable = false)
    @Builder.Default
    private Long signatureCount = 0L;

    @Column(name = "uv_initialized", nullable = false)
    @Builder.Default
    private Boolean uvInitialized = false;

    @Column(name = "backup_eligible", nullable = false)
    @Builder.Default
    private Boolean backupEligible = false;

    @Column(name = "authenticator_transports", length = 255)
    private String authenticatorTransports;

    @Column(name = "public_key_credential_type", nullable = false, length = 64)
    @Builder.Default
    private String publicKeyCredentialType = "public-key";

    @Column(name = "backup_state", nullable = false)
    @Builder.Default
    private Boolean backupState = false;

    @Lob
    @Column(name = "attestation_object", columnDefinition = "LONGBLOB")
    private byte[] attestationObject;

    @Lob
    @Column(name = "attestation_client_data_json", columnDefinition = "LONGBLOB")
    private byte[] attestationClientDataJson;

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;

    @Column(name = "last_used")
    private Instant lastUsed;

    @Column(name = "label", length = 255)
    private String label;

}
