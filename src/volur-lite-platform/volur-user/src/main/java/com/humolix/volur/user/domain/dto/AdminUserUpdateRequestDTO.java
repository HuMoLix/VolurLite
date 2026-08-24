package com.humolix.volur.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserUpdateRequestDTO {

    private String username;
    private String password;
    private String email;
    private String org;
    private String phone;
    private String bio;
    private Boolean status;
    private String roleName;
}
