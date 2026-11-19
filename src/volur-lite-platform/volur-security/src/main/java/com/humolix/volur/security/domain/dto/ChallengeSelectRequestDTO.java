package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChallengeSelectRequestDTO extends FlowRequestDTO {

    @NotBlank(message = "目标安全挑战类型不能为空")
    private String targetChallengeType;

}
