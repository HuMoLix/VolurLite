package com.humolix.volur.auth.domain.dto.recovery;

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
public class UsernameRecoverySelectRequestDTO extends FlowRequestDTO {

    @Trimmed
    @NotBlank(message = "找回方式不能为空")
    private String method;

}
