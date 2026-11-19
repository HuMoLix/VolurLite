package com.humolix.volur.auth.domain.dto.signup;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.security.domain.dto.FlowRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SignUpEtotpChallengeRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为 6 位数字")
    @Trimmed
    private String code;

}
