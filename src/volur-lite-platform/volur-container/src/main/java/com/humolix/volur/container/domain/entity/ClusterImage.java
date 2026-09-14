package com.humolix.volur.container.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * 集群 Docker 镜像持久化实体
 * 统一管理 Docker Swarm 与 K3s 中的 Docker 格式镜像
 */
@Entity
@Table(name = "cluster_image", indexes = {
        @Index(name = "idx_image_repo_tag", columnList = "repository, tag")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClusterImage implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "repository", nullable = false, length = 255)
    private String repository;

    @Builder.Default
    @Column(name = "tag", nullable = false, length = 64)
    private String tag = "latest";

    @Column(name = "image_id", length = 128)
    private String imageId;

    @Builder.Default
    @Column(name = "size_bytes", columnDefinition = "BIGINT DEFAULT 0")
    private Long sizeBytes = 0L;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private String status = "PULLED";

    @Builder.Default
    @Column(name = "in_registry", nullable = false)
    private Boolean inRegistry = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    private Instant updatedAt;

}
