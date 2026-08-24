package com.humolix.volur.challenge.entity.stage.ctf;

import com.humolix.volur.challenge.entity.Containerizable;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.framework.common.converter.AttachmentConverter;
import com.humolix.volur.framework.common.dto.AttachmentDTO;
import com.humolix.volur.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ctf_challenge")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CtfChallenge implements Containerizable, Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 128, columnDefinition = "VARCHAR(128)")
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private CtfChallengeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private User author;

    @Column(name = "flag", columnDefinition = "VARCHAR(255)")
    private String flag;

    @Column(name = "difficulty", nullable = false, columnDefinition = "INT")
    private Integer difficulty;

    @Builder.Default
    @Column(name = "is_dynamic_container", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean dynamicContainer = false;

    @Builder.Default
    @Column(name = "attachments", columnDefinition = "TEXT")
    @Convert(converter = AttachmentConverter.class)
    private List<AttachmentDTO> attachments = new ArrayList<>();

    @Column(name = "image_name", columnDefinition = "VARCHAR(255)")
    private String imageName;

    @Column(name = "container_port", columnDefinition = "INT")
    private Integer containerPort;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Override
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    @Override
    public String getAuthorName() {
        return author != null ? author.getUsername() : null;
    }

    @PrePersist
    @PreUpdate
    private void validateContainerInfo() {
        if (!this.isDynamicContainer()) {
            this.imageName = null;
            this.containerPort = null;
        }
    }

}
