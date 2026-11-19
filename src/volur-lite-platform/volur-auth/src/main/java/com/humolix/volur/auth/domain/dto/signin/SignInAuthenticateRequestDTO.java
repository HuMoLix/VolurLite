package com.humolix.volur.auth.domain.dto.signin;

import com.humolix.volur.security.domain.dto.FlowRequestDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SignInAuthenticateRequestDTO extends FlowRequestDTO {

    private Boolean rememberMe;

}
