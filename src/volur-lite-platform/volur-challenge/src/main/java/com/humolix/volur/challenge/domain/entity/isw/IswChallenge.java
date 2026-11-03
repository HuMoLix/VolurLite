package com.humolix.volur.challenge.domain.entity.isw;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.enums.ChallengeMode;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "isw_challenge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IswChallenge implements Challenge, Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "name", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 64, columnDefinition = "VARCHAR(64) DEFAULT 'PENETRATION'")
    @Builder.Default
    private String category = "PENETRATION";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", columnDefinition = "BIGINT")
    private User author;

    @Column(name = "difficulty", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer difficulty = 1;

    @Column(name = "topology_json", columnDefinition = "TEXT")
    private String topologyJson;

    @Column(name = "stage_flags_json", columnDefinition = "TEXT")
    private String stageFlagsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Override
    public String getCategoryName() {
        return category;
    }

    @Override
    public ChallengeMode getMode() {
        return ChallengeMode.ISW;
    }

}
