package com.humolix.volur.user.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_trusted_device")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTrustedDevice implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "device_hash", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String deviceHash;

    @Column(name = "device_name", length = 128, columnDefinition = "VARCHAR(128)")
    private String deviceName;

    @Column(name = "os_family", length = 32, columnDefinition = "VARCHAR(32)")
    private String osFamily;

    @Column(name = "browser_family", length = 32, columnDefinition = "VARCHAR(32)")
    private String browserFamily;

    @Column(name = "first_login_ip", length = 45, columnDefinition = "VARCHAR(45)")
    private String firstLoginIp;

    @Column(name = "last_login_ip", length = 45, columnDefinition = "VARCHAR(45)")
    private String lastLoginIp;

    @Column(name = "first_login_time")
    private Instant firstLoginTime;

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Builder.Default
    @Column(name = "is_trusted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isTrusted = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
