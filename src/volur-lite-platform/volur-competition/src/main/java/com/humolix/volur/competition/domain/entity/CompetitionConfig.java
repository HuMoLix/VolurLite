package com.humolix.volur.competition.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionConfig implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, unique = true, columnDefinition = "BIGINT")
    @JsonBackReference
    @ToString.Exclude
    private Competition competition;

    @Column(name = "max_leaders", nullable = false, columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer maxLeaders = 1;

    @Column(name = "max_team_members", nullable = false, columnDefinition = "INT DEFAULT 4")
    @Builder.Default
    private Integer maxTeamMembers = 4;

    @Column(name = "min_team_members", nullable = false, columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer minTeamMembers = 1;

    @Column(name = "max_teams", columnDefinition = "INT DEFAULT 500")
    @Builder.Default
    private Integer maxTeams = 500;

    @Column(name = "require_team_audit", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean requireTeamAudit = false;

    @Column(name = "freeze_scoreboard_minutes", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer freezeScoreboardMinutes = 0;

    @Column(name = "is_scoreboard_frozen", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean isScoreboardFrozen = false;

    @Column(name = "allow_writeup_submission", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean allowWriteupSubmission = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
