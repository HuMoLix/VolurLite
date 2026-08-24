package com.humolix.volur.challenge.entity;

import com.humolix.volur.challenge.entity.enums.ChallengeType;
import com.humolix.volur.challenge.entity.enums.SceneType;
import com.humolix.volur.challenge.entity.enums.SubmissionStatus;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "submission", indexes = {
        @Index(name = "idx_user_scene_challenge", columnList = "user_id, scene_type, scene_id, challenge_id"),
        @Index(name = "idx_scene_status", columnList = "scene_type, scene_id, status"),
        @Index(name = "idx_user_time", columnList = "user_id, submitted_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Submission implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "team_id", columnDefinition = "BIGINT")
    private Long teamId;

    @Column(name = "challenge_id", nullable = false, columnDefinition = "BIGINT")
    private Long challengeId;

    @Column(name = "scene_type", nullable = false, columnDefinition = "TINYINT")
    private SceneType sceneType;

    @Column(name = "scene_id", nullable = false, columnDefinition = "BIGINT")
    private Long sceneId;

    @Column(name = "challenge_type", nullable = false, columnDefinition = "TINYINT")
    private ChallengeType challengeType;

    @Column(name = "content", nullable = false, columnDefinition = "VARCHAR(255)")
    private String content;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private SubmissionStatus status = SubmissionStatus.PROCESSING;

    @Column(name = "ip_address", nullable = false, length = 45, columnDefinition = "VARCHAR(45)")
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, columnDefinition = "VARCHAR(255)")
    private String userAgent;

    @Builder.Default
    @Column(name = "score", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant submittedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

}
