package com.humolix.volur.security.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dsh;

    private String tl;

    private Integer cid;

    private String nextStep;

    private String status;

    private String identifier;

    private String errorMessage;

}
