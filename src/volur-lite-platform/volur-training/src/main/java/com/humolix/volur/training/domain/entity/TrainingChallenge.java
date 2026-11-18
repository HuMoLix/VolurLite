package com.humolix.volur.training.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * 训练池题目：引用题库 ctf_challenge（sourceChallengeId 唯一），不复制题目数据。
 * 分值与启停状态由训练模块独立管理。
 */
@Entity
@Table(name = "training_challenge",
        uniqueConstraints = @UniqueConstraint(name = "uk_training_source_challenge", columnNames = "source_challenge_id"))
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrainingChallenge implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "source_challenge_id", nullable = false, columnDefinition = "BIGINT")
    private Long sourceChallengeId;

    @Column(name = "points", nullable = false, columnDefinition = "INT")
    private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(32) DEFAULT 'ENABLED'")
    @Builder.Default
    private TrainingChallengeStatus status = TrainingChallengeStatus.ENABLED;

    @Column(name = "created_by", columnDefinition = "BIGINT")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
