package com.humolix.volur.verification.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "email_log", indexes = {
        @Index(name = "idx_email_log_recipient", columnList = "recipient"),
        @Index(name = "idx_email_log_type", columnList = "type"),
        @Index(name = "idx_email_log_status", columnList = "status"),
        @Index(name = "idx_email_log_message_id", columnList = "message_id"),
        @Index(name = "idx_email_log_created_at", columnList = "created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailLog implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "message_id", length = 128, columnDefinition = "VARCHAR(128)")
    private String messageId;

    @Column(name = "recipient", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String recipient;

    @Column(name = "subject", nullable = false, length = 256, columnDefinition = "VARCHAR(256)")
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private String type;

    @Column(name = "status", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sender", length = 128, columnDefinition = "VARCHAR(128)")
    private String sender;

    @Column(name = "operator_id", columnDefinition = "BIGINT")
    private Long operatorId;

    @Column(name = "operator_ip", length = 64, columnDefinition = "VARCHAR(64)")
    private String operatorIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

}
