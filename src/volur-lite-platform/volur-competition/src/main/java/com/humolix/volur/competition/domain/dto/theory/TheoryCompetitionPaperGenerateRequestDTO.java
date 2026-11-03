package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryCompetitionPaperGenerateRequestDTO {

    @NotEmpty(message = "引用的题库列表不能为空")
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

    private List<Long> selectedQuestionIds;

}
