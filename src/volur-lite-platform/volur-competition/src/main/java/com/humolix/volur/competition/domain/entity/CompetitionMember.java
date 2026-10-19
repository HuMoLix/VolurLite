package com.humolix.volur.competition.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_member", indexes = {
        @Index(name = "idx_comp_user_status", columnList = "competition_id, user_id, status"),
        @Index(name = "idx_team_status", columnList = "team_id, status"),
        @Index(name = "idx_comp_phone", columnList = "competition_id, phone"),
        @Index(name = "idx_comp_idcard", columnList = "competition_id, id_card"),
        @Index(name = "idx_comp_email", columnList = "competition_id, email")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionMember implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @ToString.Exclude
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BIGINT")
    @JsonBackReference
    @ToString.Exclude
    private CompetitionTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true, columnDefinition = "BIGINT")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private MemberStatus status = MemberStatus.INVITED;

    @Column(name = "real_name", length = 64, columnDefinition = "VARCHAR(64)")
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16, columnDefinition = "VARCHAR(16)")
    private Gender gender;

    @Column(name = "id_card", length = 32, columnDefinition = "VARCHAR(32)")
    private String idCard;

    @Column(name = "email", length = 128, columnDefinition = "VARCHAR(128)")
    private String email;

    @Column(name = "phone", length = 32, columnDefinition = "VARCHAR(32)")
    private String phone;

    @Column(name = "employee_number", length = 64, columnDefinition = "VARCHAR(64)")
    private String employeeNumber;

    @Column(name = "department", length = 128, columnDefinition = "VARCHAR(128)")
    private String department;

    @Column(name = "position", length = 64, columnDefinition = "VARCHAR(64)")
    private String position;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
