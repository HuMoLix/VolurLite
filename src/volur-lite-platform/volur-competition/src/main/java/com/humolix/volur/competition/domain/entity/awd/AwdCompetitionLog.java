package com.humolix.volur.competition.domain.entity.awd;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "awd_competition_log", indexes = {
        @Index(name = "idx_comp_awd_log_round", columnList = "competition_id, round_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AwdCompetitionLog implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @Column(name = "round_number", nullable = false, columnDefinition = "INT")
    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attacker_team_id", columnDefinition = "BIGINT")
    @ToString.Exclude
    private CompetitionTeam attackerTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "victim_team_id", columnDefinition = "BIGINT")
    @ToString.Exclude
    private CompetitionTeam victimTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", columnDefinition = "BIGINT")
    @ToString.Exclude
    private AwdCompetitionChallenge challenge;

    @Column(name = "points_delta", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer pointsDelta = 0;

    @Column(name = "log_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private String logType; // ATTACK, DEFENSE, SLA_DOWN, SLA_PASS

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
