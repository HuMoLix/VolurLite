package com.humolix.volur.competition.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.humolix.volur.competition.domain.enums.FieldType;
import com.humolix.volur.competition.domain.enums.FillScope;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "competition_field_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompetitionFieldSchema implements Serializable {

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

    @Column(name = "field_key", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String fieldKey;

    @Column(name = "field_label", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, columnDefinition = "VARCHAR(32)")
    private FieldType fieldType;

    @Column(name = "is_required", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "regex_pattern", columnDefinition = "VARCHAR(255)")
    private String regexPattern;

    @Column(name = "select_options", columnDefinition = "TEXT")
    private String selectOptions;

    @Enumerated(EnumType.STRING)
    @Column(name = "fill_scope", nullable = false, columnDefinition = "VARCHAR(32)")
    @Builder.Default
    private FillScope fillScope = FillScope.ALL_MEMBERS;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

}
