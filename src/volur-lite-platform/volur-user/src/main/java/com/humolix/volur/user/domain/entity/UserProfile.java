package com.humolix.volur.user.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.humolix.volur.user.domain.enums.PrivacyType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "user_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserProfile implements Serializable {

    @Id
    @Column(name = "user_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "org", length = 128, columnDefinition = "VARCHAR(128)")
    private String org;

    @Column(name = "major", length = 128)
    private String major;

    @Column(name = "study_duration")
    @JsonIgnore
    private Double studyDuration;

    @Column(name = "department", length = 128)
    @JsonIgnore
    private String department;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", columnDefinition = "VARCHAR(255)")
    private String avatarUrl;

    @Column(name = "banner_url", columnDefinition = "VARCHAR(255)")
    private String bannerUrl;

    @Column(name = "notice", columnDefinition = "TEXT")
    private String notice;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isHidden = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "email_privacy", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PRIVATE'")
    private PrivacyType emailPrivacy = PrivacyType.PRIVATE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "phone_privacy", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PRIVATE'")
    private PrivacyType phonePrivacy = PrivacyType.PRIVATE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "org_privacy", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PUBLIC'")
    private PrivacyType orgPrivacy = PrivacyType.PUBLIC;

}
