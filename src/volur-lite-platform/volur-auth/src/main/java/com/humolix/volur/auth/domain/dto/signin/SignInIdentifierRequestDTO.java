package com.humolix.volur.auth.domain.dto.signin;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.security.domain.dto.FlowRequestDTO;
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
public class SignInIdentifierRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "登录账号或邮箱不能为空")
    @Trimmed
    private String identifier;

}
