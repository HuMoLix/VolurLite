package com.humolix.volur.competition.domain.entity;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Competition implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "title", nullable = false, length = 128, columnDefinition = "VARCHAR(128)")
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 64, columnDefinition = "VARCHAR(64)")
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_url", columnDefinition = "VARCHAR(255)")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private CompetitionType type = CompetitionType.CTF;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private CompetitionStatus status = CompetitionStatus.DRAFT;

    @Column(name = "reg_start_time")
    private Instant regStartTime;

    @Column(name = "reg_end_time")
    private Instant regEndTime;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", columnDefinition = "BIGINT")
    private User createdBy;

    @OneToOne(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private CompetitionConfig config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    public CompetitionStatus resolveStatusByTime() {
        if (this.status == null || this.status == CompetitionStatus.DRAFT || this.status == CompetitionStatus.ARCHIVED || this.status == CompetitionStatus.HIDDEN) {
            return this.status != null ? this.status : CompetitionStatus.DRAFT;
        }
        Instant now = Instant.now();
        if (this.endTime != null && now.isAfter(this.endTime)) {
            return CompetitionStatus.ENDED;
        }
        if (this.startTime != null && !now.isBefore(this.startTime)) {
            return CompetitionStatus.RUNNING;
        }

        boolean hasRegStart = this.regStartTime != null;
        boolean hasRegEnd = this.regEndTime != null;
        boolean afterRegStart = !hasRegStart || !now.isBefore(this.regStartTime);
        boolean beforeRegEnd = !hasRegEnd || !now.isAfter(this.regEndTime);

        if (afterRegStart && beforeRegEnd) {
            return CompetitionStatus.REGISTRATION;
        }
        return CompetitionStatus.PUBLISHED;
    }

    public boolean refreshStatus() {
        CompetitionStatus resolved = resolveStatusByTime();
        if (resolved != null && resolved != this.status) {
            this.status = resolved;
            return true;
        }
        return false;
    }

}
