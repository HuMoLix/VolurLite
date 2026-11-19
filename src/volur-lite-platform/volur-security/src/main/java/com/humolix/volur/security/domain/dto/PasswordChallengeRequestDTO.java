package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordChallengeRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "密码不能为空")
    private String password;

    private Boolean rememberMe;

}
