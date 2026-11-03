package com.humolix.volur.user.domain.vo;

import com.humolix.volur.user.domain.enums.Gender;
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
public class AdminUserDetailsVO implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private Gender gender;
    private LocalDate birthday;
    private String role;
    private String org;
    private String major;
    private String bio;
    private String avatarUrl;
    private Boolean status;
    private Boolean isHidden;
    private String lastLoginIp;
    private Instant createdAt;
    private Instant updatedAt;

}
