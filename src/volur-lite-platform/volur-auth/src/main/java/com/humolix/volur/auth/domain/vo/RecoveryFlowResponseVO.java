package com.humolix.volur.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoveryFlowResponseVO extends FlowResponseVO {

    @Serial
    private static final long serialVersionUID = 1L;

}
