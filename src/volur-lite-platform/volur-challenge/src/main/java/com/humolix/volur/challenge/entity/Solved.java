package com.humolix.volur.challenge.entity;

import com.humolix.volur.challenge.entity.enums.ChallengeType;
import com.humolix.volur.challenge.entity.enums.SceneType;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "solved", indexes = {
        @Index(name = "idx_user_scene", columnList = "user_id, scene_type, scene_id"),
        @Index(name = "idx_user_challenge", columnList = "user_id, challenge_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Solved implements Serializable {

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

    @Builder.Default
    @Column(name = "score", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;

    @CreationTimestamp
    @Column(name = "solved_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant solvedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

}
