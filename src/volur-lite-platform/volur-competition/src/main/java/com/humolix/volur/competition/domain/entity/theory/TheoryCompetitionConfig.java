package com.humolix.volur.competition.domain.entity.theory;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "theory_competition_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TheoryCompetitionConfig implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, unique = true, columnDefinition = "BIGINT")
    private Competition competition;

    @Column(name = "bank_ids", length = 1024, columnDefinition = "VARCHAR(1024)")
    private String bankIds;

    @Column(name = "single_choice_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer singleChoiceCount = 0;

    @Column(name = "single_choice_score", nullable = false, columnDefinition = "INT DEFAULT 2")
    @Builder.Default
    private Integer singleChoiceScore = 2;

    @Column(name = "multiple_choice_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer multipleChoiceCount = 0;

    @Column(name = "multiple_choice_score", nullable = false, columnDefinition = "INT DEFAULT 4")
    @Builder.Default
    private Integer multipleChoiceScore = 4;

    @Column(name = "true_false_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer trueFalseCount = 0;

    @Column(name = "true_false_score", nullable = false, columnDefinition = "INT DEFAULT 2")
    @Builder.Default
    private Integer trueFalseScore = 2;

    @Column(name = "essay_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer essayCount = 0;

    @Column(name = "essay_score", nullable = false, columnDefinition = "INT DEFAULT 10")
    @Builder.Default
    private Integer essayScore = 10;

    @Column(name = "is_random", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isRandom = true;

    @Column(name = "shuffle_options", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean shuffleOptions = true;

    @Column(name = "total_questions", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "total_score", nullable = false, columnDefinition = "INT DEFAULT 100")
    @Builder.Default
    private Integer totalScore = 100;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
