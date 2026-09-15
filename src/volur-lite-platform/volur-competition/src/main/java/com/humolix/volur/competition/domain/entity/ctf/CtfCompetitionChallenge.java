package com.humolix.volur.competition.domain.entity.ctf;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ctf_competition_challenge", indexes = {
        @Index(name = "idx_comp_ctf_chal", columnList = "competition_id, challenge_id")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CtfCompetitionChallenge implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "challenge_id", nullable = false, columnDefinition = "BIGINT")
    private CtfChallenge challenge;

    @Column(name = "initial_points", nullable = false, columnDefinition = "INT DEFAULT 1000")
    @Builder.Default
    private Integer initialPoints = 1000;

    @Column(name = "min_points", nullable = false, columnDefinition = "INT DEFAULT 100")
    @Builder.Default
    private Integer minPoints = 100;

    @Column(name = "decay_count", nullable = false, columnDefinition = "INT DEFAULT 30")
    @Builder.Default
    private Integer decayCount = 30;

    @Column(name = "current_points", nullable = false, columnDefinition = "INT DEFAULT 1000")
    @Builder.Default
    private Integer currentPoints = 1000;

    @Column(name = "solves_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer solvesCount = 0;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
