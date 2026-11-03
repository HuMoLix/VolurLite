package com.humolix.volur.challenge.domain.entity.ctf;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeMode;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ctf_challenge")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CtfChallenge implements Challenge, Containerizable, Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "name", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false, columnDefinition = "BIGINT")
    private CtfChallengeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, columnDefinition = "BIGINT")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(32)")
    private ChallengeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "VARCHAR(32) DEFAULT 'EASY'")
    @Builder.Default
    private ChallengeDifficulty difficulty = ChallengeDifficulty.EASY;

    @Column(name = "flag", columnDefinition = "VARCHAR(255)")
    private String flag;

    @Column(name = "attachment_url", columnDefinition = "VARCHAR(255)")
    private String attachmentUrl;

    @Column(name = "attachment_name", columnDefinition = "VARCHAR(255)")
    private String attachmentName;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "image_name", columnDefinition = "VARCHAR(255)")
    private String imageName;

    @Column(name = "container_port", columnDefinition = "INT")
    private Integer containerPort;

    @Column(name = "cpu_limit", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer cpuLimit = 1;

    @Column(name = "memory_limit", columnDefinition = "INT DEFAULT 128")
    @Builder.Default
    private Integer memoryLimit = 128;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Override
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    @Override
    public ChallengeMode getMode() {
        return ChallengeMode.CTF;
    }

}
