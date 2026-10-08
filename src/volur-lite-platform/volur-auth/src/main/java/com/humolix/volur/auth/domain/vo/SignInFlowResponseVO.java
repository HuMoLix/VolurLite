package com.humolix.volur.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignInFlowResponseVO extends FlowResponseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    private String captchaType;

    private String username;

    private String email;

    private String displayName;

    private String avatarUrl;

}
