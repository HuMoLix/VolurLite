package com.humolix.volur.security.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
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
public class TotpEnableRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "动态验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "动态验证码必须为6位数字")
    @Trimmed
    private String code;

}
