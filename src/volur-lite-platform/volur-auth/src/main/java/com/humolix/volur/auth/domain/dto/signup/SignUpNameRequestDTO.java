package com.humolix.volur.auth.domain.dto.signup;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.security.domain.dto.FlowRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class SignUpNameRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(
            regexp = "^(\\$V1\\$GCM\\$.+|[a-zA-Z0-9_-]{3,32})$",
            message = "用户名长度必须在3到32个字符之间，用户名只能包含字母、数字、下划线和连字符"
    )
    @Trimmed
    private String username;

}

