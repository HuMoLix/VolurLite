package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "competition_team")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionTeam implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @Column(name = "team_name", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String teamName;

    @Column(name = "avatar_url", columnDefinition = "VARCHAR(255)")
    private String avatarUrl;

    @Column(name = "description", columnDefinition = "VARCHAR(512)")
    private String description;

    @Column(name = "organization", length = 128, columnDefinition = "VARCHAR(128)")
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private com.humolix.volur.competition.domain.enums.OrganizationType organizationType = com.humolix.volur.competition.domain.enums.OrganizationType.SCHOOL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_id", nullable = false, columnDefinition = "BIGINT")
    private User captain;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private TeamStatus status = TeamStatus.AUDIT_DRAFT;

    @Column(name = "audit_reason", columnDefinition = "TEXT")
    private String auditReason;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<CompetitionMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
