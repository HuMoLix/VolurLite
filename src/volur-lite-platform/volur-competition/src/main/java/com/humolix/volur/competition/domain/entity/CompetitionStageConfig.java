package com.humolix.volur.competition.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.StageStatus;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_stage_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionStageConfig implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false, columnDefinition = "BIGINT")
    @JsonBackReference
    @ToString.Exclude
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, columnDefinition = "VARCHAR(32)")
    private CompetitionStage stage;

    @Column(name = "stage_name", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String stageName;

    @Column(name = "stage_order", nullable = false, columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer stageOrder = 1;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "weight_percent", nullable = false, columnDefinition = "DOUBLE DEFAULT 100.0")
    @Builder.Default
    private Double weightPercent = 100.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private StageStatus status = StageStatus.PENDING;

    @Column(name = "stage_params_json", columnDefinition = "TEXT")
    private String stageParamsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
