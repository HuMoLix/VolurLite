package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_registration_data", indexes = {
        @Index(name = "idx_reg_comp_team_user", columnList = "competition_id, team_id, user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionRegistrationData implements Serializable {

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
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CompetitionTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private User user;

    @Column(name = "field_key", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String fieldKey;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
