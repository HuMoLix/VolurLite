package com.humolix.volur.challenge.domain.entity.awd;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.challenge.domain.enums.ChallengeMode;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "awd_challenge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AwdChallenge implements Challenge, Containerizable, Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "name", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "VARCHAR(32)")
    private AwdCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", columnDefinition = "BIGINT")
    private User author;

    @Column(name = "difficulty", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer difficulty = 1;

    @Column(name = "attachment_url", columnDefinition = "VARCHAR(255)")
    private String attachmentUrl;

    @Column(name = "image_name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String imageName;

    @Column(name = "container_port", nullable = false, columnDefinition = "INT")
    private Integer containerPort;

    @Column(name = "check_script_url", columnDefinition = "TEXT")
    private String checkScriptUrl;

    @Column(name = "flag_path", columnDefinition = "VARCHAR(255) DEFAULT '/flag'")
    @Builder.Default
    private String flagPath = "/flag";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Override
    public String getCategoryName() {
        return category != null ? category.name() : null;
    }

    @Override
    public ChallengeMode getMode() {
        return ChallengeMode.AWD;
    }

}
