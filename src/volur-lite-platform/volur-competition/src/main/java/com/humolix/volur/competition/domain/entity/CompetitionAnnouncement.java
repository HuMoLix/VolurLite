package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_announcement", indexes = {
        @Index(name = "idx_comp_anno_comp_created", columnList = "competition_id, created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionAnnouncement implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    private Long competitionId;

    @Column(name = "title", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "level", nullable = false, length = 32, columnDefinition = "VARCHAR(32) DEFAULT 'INFO'")
    @Builder.Default
    private String level = "INFO";

    @Column(name = "is_pinned", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "challenge_id", columnDefinition = "BIGINT")
    private Long challengeId;

    @Column(name = "challenge_name", length = 128, columnDefinition = "VARCHAR(128)")
    private String challengeName;

    @Column(name = "created_by_id", columnDefinition = "BIGINT")
    private Long createdById;

    @Column(name = "created_by_name", length = 64, columnDefinition = "VARCHAR(64)")
    private String createdByName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
