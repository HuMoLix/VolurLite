package com.humolix.volur.notification.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.notification.domain.enums.BroadcastDisplayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "broadcast_message", indexes = {
        @Index(name = "idx_broadcast_created_at", columnList = "created_at"),
        @Index(name = "idx_broadcast_category_created", columnList = "category, created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BroadcastMessage implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "publisher_id", columnDefinition = "BIGINT")
    private Long publisherId;

    @Column(name = "publisher_name", length = 64, columnDefinition = "VARCHAR(64)")
    private String publisherName;

    @Column(name = "title", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private BroadcastDisplayType displayType = BroadcastDisplayType.MODAL;

    @Column(name = "cover_image_url", length = 512, columnDefinition = "VARCHAR(512)")
    private String coverImageUrl;

    @Column(name = "link_url", length = 512, columnDefinition = "VARCHAR(512)")
    private String linkUrl;

    @Column(name = "force_popup", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean forcePopup = false;

    @Column(name = "priority", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "expire_time", columnDefinition = "TIMESTAMP")
    private Instant expireTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
