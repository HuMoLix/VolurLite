package com.humolix.volur.training.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * 训练解题记录：同一用户同一题只记一次分（user_id + source_challenge_id 唯一）。
 * 训练池移除题目不删记录，重新导入同题后已解状态与解出数自动恢复。
 */
@Entity
@Table(name = "training_solve_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_training_solve_user_challenge", columnNames = {"user_id", "source_challenge_id"}),
        indexes = {
                @Index(name = "idx_training_solve_user", columnList = "user_id"),
                @Index(name = "idx_training_solve_challenge", columnList = "source_challenge_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrainingSolveRecord implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "source_challenge_id", nullable = false, columnDefinition = "BIGINT")
    private Long sourceChallengeId;

    /** 解出时刻的分值快照（题目分值后续调整不影响已记录得分） */
    @Column(name = "points_earned", nullable = false, columnDefinition = "INT")
    private Integer pointsEarned;

    @CreationTimestamp
    @Column(name = "solved_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant solvedAt;

}
