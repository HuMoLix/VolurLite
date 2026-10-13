package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryCompetitionConfigDTO {

    @NotEmpty(message = "请选择至少一个引用的理论题库")
    private List<Long> bankIds;

    private Integer singleChoiceCount;

    private Integer singleChoiceScore;

    private Integer multipleChoiceCount;

    private Integer multipleChoiceScore;

    private Integer trueFalseCount;

    private Integer trueFalseScore;

    private Integer essayCount;

    private Integer essayScore;

    private Boolean isRandom;

    private Boolean shuffleOptions;

}
