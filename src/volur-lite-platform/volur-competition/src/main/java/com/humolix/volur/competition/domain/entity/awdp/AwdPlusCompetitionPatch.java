package com.humolix.volur.competition.domain.entity.awdp;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "awd_plus_competition_patch", indexes = {
        @Index(name = "idx_comp_awdp_patch_team", columnList = "competition_id, team_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AwdPlusCompetitionPatch implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private AwdPlusCompetitionChallenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CompetitionTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private User user;

    @Column(name = "patch_file_url", nullable = false, columnDefinition = "VARCHAR(255)")
    private String patchFileUrl;

    @Column(name = "verify_status", nullable = false, length = 32, columnDefinition = "VARCHAR(32) DEFAULT 'PENDING'")
    @Builder.Default
    private String verifyStatus = "PENDING"; // PENDING, PASSED, FAILED

    @Column(name = "verify_message", columnDefinition = "TEXT")
    private String verifyMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
