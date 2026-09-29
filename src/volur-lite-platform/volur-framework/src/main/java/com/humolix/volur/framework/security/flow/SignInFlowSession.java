package com.humolix.volur.framework.security.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 历史兼容 SignIn 阶段化会话模型 (建议统一采用通用 {@link AuthFlowSession})
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SignInFlowSession extends AuthFlowSession {

    @Serial
    private static final long serialVersionUID = 1L;

}
