package com.humolix.volur.auth.domain.dto.signup;

import com.humolix.volur.security.domain.dto.FlowRequestDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SignUpCompleteRequestDTO extends FlowRequestDTO {

    @Builder.Default
    private Boolean skipOptions = true;

    private List<String> selectedOptions;

}
