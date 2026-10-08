package com.humolix.volur.security.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReCAPTCHAChallengeSubmitDTO extends FlowRequestDTO {

    @NotBlank(message = "人机验证凭据不能为空")
    private String recaptchaTicket;

    @Trimmed
    private String recoveryType;

}
