package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasskeyAssertionVerifyRequestDTO extends FlowRequestDTO {

    @NotNull(message = "通行密钥凭据不能为空")
    private Map<String, Object> credential;

    private Boolean rememberMe;

}
