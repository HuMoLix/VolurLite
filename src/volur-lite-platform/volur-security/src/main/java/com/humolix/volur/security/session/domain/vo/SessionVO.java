package com.humolix.volur.security.session.domain.vo;

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
public class SessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dsh;

    private String tl;

    private String flowName;

    private String flowEntry;

    private Integer cid;

    private String fp;

    private Long serverTime;

}
