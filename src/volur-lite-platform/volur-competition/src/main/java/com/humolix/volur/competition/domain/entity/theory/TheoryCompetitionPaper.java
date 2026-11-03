package com.humolix.volur.competition.domain.entity.theory;

import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "theory_competition_paper", indexes = {
        @Index(name = "idx_comp_theory_chal", columnList = "competition_id, challenge_id"),
        @Index(name = "idx_paper_user_paper", columnList = "user_paper_id"),
        @Index(name = "idx_paper_user_comp", columnList = "competition_id, user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TheoryCompetitionPaper implements Serializable {

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
    @JoinColumn(name = "user_paper_id", columnDefinition = "BIGINT")
    @ToString.Exclude
    private TheoryUserPaper userPaper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", columnDefinition = "BIGINT")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "challenge_id", nullable = false, columnDefinition = "BIGINT")
    private TheoryChallenge challenge;

    @Column(name = "score", nullable = false, columnDefinition = "INT DEFAULT 5")
    @Builder.Default
    private Integer score = 5;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "shuffled_options", columnDefinition = "TEXT")
    private String shuffledOptions;

    @Column(name = "shuffled_standard_answer", columnDefinition = "TEXT")
    private String shuffledStandardAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
