package com.humolix.volur.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_user_total_points", columnList = "total_points")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64, columnDefinition = "VARCHAR(64)")
    private String username;

    @Column(name = "password", nullable = false, columnDefinition = "VARCHAR(255)")
    private String password;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "VARCHAR(255)")
    private String email;

    @Column(name = "phone", unique = true, length = 20, columnDefinition = "VARCHAR(20)")
    private String phone;

    @Column(name = "real_name", length = 64)
    @JsonIgnore
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false, columnDefinition = "BIGINT")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @ToString.Exclude
    private UserProfile userProfile;

    @Builder.Default
    @Column(name = "total_points", nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double totalPoints = 0.0;

    @Builder.Default
    @Column(name = "solves", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long solves = 0L;

    @Column(name = "last_login_ip", length = 45, columnDefinition = "VARCHAR(45)")
    private String lastLoginIp;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isHidden = false;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean status = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
