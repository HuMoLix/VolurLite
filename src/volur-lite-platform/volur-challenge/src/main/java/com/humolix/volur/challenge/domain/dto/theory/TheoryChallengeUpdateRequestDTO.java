package com.humolix.volur.challenge.domain.dto.theory;

import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryChallengeUpdateRequestDTO {

    @Trimmed
    private String name;

    @Trimmed
    private String category;

    private TheoryType type;

    private String content;

    private String options;

    @Trimmed
    private String standardAnswer;

    private String explanation;

    private Integer score;

    private Integer difficulty;

    private Long bankId;

}
