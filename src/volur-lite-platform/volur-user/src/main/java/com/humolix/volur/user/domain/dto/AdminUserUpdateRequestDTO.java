package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserUpdateRequestDTO {

    @Trimmed
    private String username;
    private String password;
    @Trimmed(toLowerCase = true)
    private String email;
    @Trimmed
    private String org;
    @Trimmed
    private String phone;
    @Trimmed
    private String bio;
    private Boolean status;
    @Trimmed
    private String roleName;
}
