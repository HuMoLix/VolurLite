package com.humolix.volur.user.domain.vo;

import com.humolix.volur.user.domain.enums.Gender;
import com.humolix.volur.user.domain.enums.PrivacyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseVO implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate birthday;
    private String role;
    private String org;
    private String major;
    private String bio;
    private String avatarUrl;
    private String bannerUrl;
    private String notice;
    private PrivacyType emailPrivacy;
    private PrivacyType phonePrivacy;
    private PrivacyType orgPrivacy;
    private Instant createdAt;

}
