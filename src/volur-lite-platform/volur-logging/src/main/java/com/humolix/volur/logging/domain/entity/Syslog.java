package com.humolix.volur.logging.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "sys_log", indexes = {
        @Index(name = "idx_syslog_created_at", columnList = "created_at"),
        @Index(name = "idx_syslog_level_module", columnList = "level, module"),
        @Index(name = "idx_syslog_log_type", columnList = "log_type"),
        @Index(name = "idx_syslog_operator", columnList = "operator"),
        @Index(name = "idx_syslog_trace_id", columnList = "trace_id")
})
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Syslog implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "level", length = 20, nullable = false)
    private String level;

    @Column(name = "log_type", length = 50)
    private String logType;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "action", length = 200)
    private String action;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_ip", length = 50)
    private String operatorIp;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "request_method", length = 20)
    private String requestMethod;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private boolean status = true;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
