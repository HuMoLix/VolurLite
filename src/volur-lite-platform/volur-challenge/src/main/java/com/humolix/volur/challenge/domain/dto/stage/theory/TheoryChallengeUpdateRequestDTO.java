package com.humolix.volur.challenge.domain.dto.stage.theory;

import com.humolix.volur.challenge.domain.enums.TheoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryChallengeUpdateRequestDTO {

    private String name;

    private String category;

    private TheoryType type;

    private String content;

    private String options;

    private String standardAnswer;

    private String explanation;

    private Integer score;

    private Integer difficulty;

}
