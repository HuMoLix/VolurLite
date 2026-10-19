package com.humolix.volur.verification.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.verification.domain.enums.IpRuleSource;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "risk_ip_rule", indexes = {
        @Index(name = "idx_risk_ip_rule_ip_type_status", columnList = "ip, rule_type, status"),
        @Index(name = "idx_risk_ip_rule_status_expires", columnList = "status, expires_at"),
        @Index(name = "idx_risk_ip_rule_created_at", columnList = "created_at")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RiskIpRule implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "ip", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private IpRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private IpRuleSource sourceType;

    @Column(name = "reason", length = 255, columnDefinition = "VARCHAR(255)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private IpRuleStatus status;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL")
    private Instant expiresAt;

    @Column(name = "operator_id", columnDefinition = "BIGINT")
    private Long operatorId;

    @Column(name = "operator_name", length = 64, columnDefinition = "VARCHAR(64)")
    private String operatorName;

    @Column(name = "operator_ip", length = 64, columnDefinition = "VARCHAR(64)")
    private String operatorIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant updatedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isEffective() {
        return status == IpRuleStatus.ACTIVE && !isExpired();
    }
}
