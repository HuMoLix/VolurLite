package com.humolix.volur.user.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryVO implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String role;
    private String org;
    private String bio;
    private Instant createdAt;

}
