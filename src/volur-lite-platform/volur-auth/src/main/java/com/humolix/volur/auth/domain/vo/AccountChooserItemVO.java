package com.humolix.volur.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountChooserItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String identifier;

    private String username;

    private String email;

    private String displayName;

    private String avatarUrl;

    private String sessionStatus;

}
