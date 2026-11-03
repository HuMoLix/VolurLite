package com.humolix.volur.competition.domain.entity.awdp;

import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "awd_plus_competition_challenge", indexes = {
        @Index(name = "idx_comp_awdp_chal", columnList = "competition_id, challenge_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AwdPlusCompetitionChallenge implements Serializable {

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
    private AwdPlusChallenge challenge;

    @Column(name = "initial_points", nullable = false, columnDefinition = "INT DEFAULT 1000")
    @Builder.Default
    private Integer initialPoints = 1000;

    @Column(name = "current_points", nullable = false, columnDefinition = "INT DEFAULT 1000")
    @Builder.Default
    private Integer currentPoints = 1000;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
