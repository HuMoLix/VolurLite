package com.humolix.volur.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsernameRecoveryOptionsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<RecoveryMethodVO> methods = new ArrayList<>();

}
