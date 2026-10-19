package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * 竞赛运行操作专属日志实体
 * 隔离于系统全局审计日志，记录单场竞赛内选手启停容器、提交 Flag 等关键事件。
 */
@Entity
@Table(name = "competition_operation_log", indexes = {
        @Index(name = "idx_comp_op_log_comp_created", columnList = "competition_id, created_at DESC"),
        @Index(name = "idx_comp_op_log_comp_user", columnList = "competition_id, username"),
        @Index(name = "idx_comp_op_log_comp_team", columnList = "competition_id, team_name"),
        @Index(name = "idx_comp_op_log_comp_challenge", columnList = "competition_id, challenge_title"),
        @Index(name = "idx_comp_op_log_comp_type", columnList = "competition_id, log_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionOperationLog implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    private Long competitionId;

    @Column(name = "user_id", columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "team_id", columnDefinition = "BIGINT")
    private Long teamId;

    @Column(name = "team_name", length = 128)
    private String teamName;

    @Column(name = "challenge_id", columnDefinition = "BIGINT")
    private Long challengeId;

    @Column(name = "challenge_title", length = 128)
    private String challengeTitle;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "log_type", nullable = false, length = 32)
    private String logType; // CONTAINER, FLAG, TEAM, DEFENSE, THEORY

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "operator_ip", length = 64)
    private String operatorIp;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "request_uri", length = 255)
    private String requestUri;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

}
