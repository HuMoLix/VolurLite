package com.humolix.volur.container.entity;

import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.container.entity.enums.InstanceStartupStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ctf_challenge_instance", indexes = {
        @Index(name = "idx_ctf_inst_user_chal", columnList = "target_id, challenge_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CtfChallengeInstance implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Builder.Default
    @Column(name = "stage_id", columnDefinition = "BIGINT")
    private Long stageId = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CtfChallenge challenge;

    @Column(name = "target_id", nullable = false, columnDefinition = "BIGINT")
    private Long targetId;

    @Column(name = "container_id", columnDefinition = "VARCHAR(128)")
    private String containerId;

    @Column(name = "access_url", nullable = false, columnDefinition = "VARCHAR(255)")
    private String accessUrl;

    @Column(name = "flag", nullable = false, columnDefinition = "VARCHAR(255)")
    private String flag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InstanceStartupStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant expiresAt;

}
