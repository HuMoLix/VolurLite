package com.humolix.volur.challenge.domain.entity.theory;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.enums.ChallengeMode;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "theory_challenge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TheoryChallenge implements Challenge, Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "name", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String name;

    @Column(name = "category", length = 64, columnDefinition = "VARCHAR(64)")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(32)")
    private TheoryType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "standard_answer", nullable = false, columnDefinition = "TEXT")
    private String standardAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "score", nullable = false, columnDefinition = "INT DEFAULT 5")
    @Builder.Default
    private Integer score = 5;

    @Column(name = "difficulty", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer difficulty = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", columnDefinition = "BIGINT")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", columnDefinition = "BIGINT")
    private TheoryBank bank;

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
        return ChallengeMode.THEORY;
    }

}
