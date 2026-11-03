package com.humolix.volur.user.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsVO implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String role;
    private List<String> authorities;
    private Boolean status;
    private Instant createdAt;

}
