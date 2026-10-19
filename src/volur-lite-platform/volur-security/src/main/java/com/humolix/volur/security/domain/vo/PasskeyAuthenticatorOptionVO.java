package com.humolix.volur.security.domain.vo;

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
public class PasskeyAuthenticatorOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;

    private String name;

    private String attachment;

    private String description;

    private String icon;

    @Builder.Default
    private Boolean available = true;

}
