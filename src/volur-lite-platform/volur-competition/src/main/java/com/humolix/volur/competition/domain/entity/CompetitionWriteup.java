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
@Table(name = "competition_writeup", indexes = {
        @Index(name = "idx_comp_wp_comp_team", columnList = "competition_id, team_id"),
        @Index(name = "idx_comp_wp_created", columnList = "created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionWriteup implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    private Long competitionId;

    @Column(name = "team_id", nullable = false, columnDefinition = "BIGINT")
    private Long teamId;

    @Column(name = "team_name", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String teamName;

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "username", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String username;

    @Column(name = "file_name", nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 512, columnDefinition = "VARCHAR(512)")
    private String fileUrl;

    @Column(name = "file_size", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "file_type", length = 32, columnDefinition = "VARCHAR(32)")
    private String fileType;

    @Column(name = "status", nullable = false, length = 32, columnDefinition = "VARCHAR(32) DEFAULT 'SUBMITTED'")
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "score", columnDefinition = "INT")
    private Integer score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
