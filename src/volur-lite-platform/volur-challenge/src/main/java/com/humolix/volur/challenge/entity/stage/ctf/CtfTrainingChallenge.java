package com.humolix.volur.challenge.entity.stage.ctf;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ctf_training_challenge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CtfTrainingChallenge implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, unique = true, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CtfChallenge challenge;

    @Column(name = "initial_points", nullable = false)
    private Integer initialPoints;

    @Column(name = "minimum_points", nullable = false)
    private Integer minimumPoints;

    @Column(name = "current_points", nullable = false)
    private Integer currentPoints;

    @Builder.Default
    @Column(name = "solve_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer solveCount = 0;

    @Builder.Default
    @Column(name = "is_dynamic_score", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean dynamicScore = false;

    @Builder.Default
    @Column(name = "decay_coefficient", columnDefinition = "FLOAT DEFAULT 0.5")
    private Float decayCoefficient = 0.5f;

    @Builder.Default
    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean active = false;

    @Builder.Default
    @Column(name = "cpu_limit", columnDefinition = "INT DEFAULT 1")
    private Integer cpuLimit = 1;

    @Builder.Default
    @Column(name = "memory_limit", columnDefinition = "INT DEFAULT 128")
    private Integer memoryLimit = 128;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    @PreUpdate
    private void validateLimits() {
        if (this.challenge != null && !this.challenge.isDynamicContainer()) {
            this.cpuLimit = null;
            this.memoryLimit = null;
        }
    }

}
