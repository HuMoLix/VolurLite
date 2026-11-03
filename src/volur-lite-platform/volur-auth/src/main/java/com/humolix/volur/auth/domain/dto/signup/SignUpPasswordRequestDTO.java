package com.humolix.volur.auth.domain.dto.signup;

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
public class SignUpPasswordRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "密码不能为空")
    @Pattern(
            regexp = "^(\\$V1\\$GCM\\$.+|(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32})$",
            message = "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间"
    )
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Pattern(
            regexp = "^(\\$V1\\$GCM\\$.+|(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32})$",
            message = "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间"
    )
    private String confirmPassword;

}

