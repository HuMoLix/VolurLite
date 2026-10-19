package com.humolix.volur.auth.domain.dto.recovery;

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
public class UsernameRecoveryEmailRequestDTO extends FlowRequestDTO {

    @Trimmed(toLowerCase = true)
    @NotBlank(message = "电子邮箱不能为空")
    @Email(message = "邮箱格式不正确或邮件地址不合法")
    private String email;

}

