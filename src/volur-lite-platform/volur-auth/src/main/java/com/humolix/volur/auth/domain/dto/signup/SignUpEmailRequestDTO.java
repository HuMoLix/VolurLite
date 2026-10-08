package com.humolix.volur.auth.domain.dto.signup;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.framework.common.validation.annotation.Email;
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
public class SignUpEmailRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确或邮件地址不合法")
    @Trimmed(toLowerCase = true)
    private String email;

}

