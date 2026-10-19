package com.humolix.volur.notification.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_receiver_status_created", columnList = "receiver_id, status, created_at"),
        @Index(name = "idx_receiver_type", columnList = "receiver_id, type")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "sender_id", columnDefinition = "BIGINT")
    private Long senderId;

    @Column(name = "receiver_id", nullable = false, columnDefinition = "BIGINT")
    private Long receiverId;

    @Column(name = "title", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private NotificationType type = NotificationType.ACCOUNT_CHANGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
    @Builder.Default
    private NotificationActionStatus actionStatus = NotificationActionStatus.NONE;

    @Column(name = "action_data", columnDefinition = "JSON")
    private String actionData;

    @Column(name = "read_time", columnDefinition = "TIMESTAMP")
    private Instant readTime;

    @Column(name = "expire_time", columnDefinition = "TIMESTAMP")
    private Instant expireTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
