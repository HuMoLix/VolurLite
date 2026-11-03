package com.humolix.volur.container.domain.entity;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ctf_challenge_instance", indexes = {
        @Index(name = "idx_ctf_inst_user_chal", columnList = "target_id, challenge_id"),
        @Index(name = "idx_ctf_inst_team_chal", columnList = "team_id, challenge_id")
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

    @Column(name = "team_id", columnDefinition = "BIGINT")
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = true, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CtfChallenge challenge;

    @Column(name = "image_name", columnDefinition = "VARCHAR(255)")
    private String imageName;

    @Column(name = "container_name", columnDefinition = "VARCHAR(128)")
    private String containerName;

    @Column(name = "ports", columnDefinition = "VARCHAR(512)")
    private String ports;

    @Column(name = "volumes", columnDefinition = "VARCHAR(1024)")
    private String volumes;

    @Column(name = "env_vars", columnDefinition = "VARCHAR(2048)")
    private String envVars;

    @Column(name = "target_id", nullable = false, columnDefinition = "BIGINT")
    private Long targetId;

    @Column(name = "container_id", columnDefinition = "VARCHAR(128)")
    private String containerId;

    @Column(name = "access_url", nullable = false, columnDefinition = "VARCHAR(255)")
    private String accessUrl;

    @Column(name = "node_ip", columnDefinition = "VARCHAR(64)")
    private String nodeIp;

    @Column(name = "flag", nullable = true, columnDefinition = "VARCHAR(255)")
    private String flag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InstanceStartupStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "container_type", columnDefinition = "VARCHAR(32)")
    private String containerType;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant expiresAt;

}
