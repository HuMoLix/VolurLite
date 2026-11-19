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
public class EmailOTPChallengeRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "邮箱验证码不能为空")
    private String code;

    private Boolean rememberMe;

}
