package com.humolix.volur.auth.domain.vo;

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
public class SignUpOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String type;

    private String title;

    private String subtitle;

    private String icon;

    private String tag;

    @Builder.Default
    private Boolean available = true;

}
