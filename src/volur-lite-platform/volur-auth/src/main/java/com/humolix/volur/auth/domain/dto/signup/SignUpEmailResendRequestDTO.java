package com.humolix.volur.auth.domain.dto.signup;

import com.humolix.volur.security.domain.dto.FlowRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SignUpEmailResendRequestDTO extends FlowRequestDTO {

}
